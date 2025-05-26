/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.momo.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fineract.infrastructure.configuration.api.GlobalConfigurationConstants;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.infrastructure.momo.data.YoPaymentRequest;
import org.apache.fineract.infrastructure.momo.data.YoPaymentResponse;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentRepaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentRepaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.util.YoPaymentXmlUtil;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementation of the YoPaymentRepaymentService for processing loan repayments via Yo Payment
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YoPaymentRepaymentServiceImpl implements YoPaymentRepaymentService {

    private final Environment env;
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final YoPaymentRepaymentTransactionRepository yoPaymentRepaymentTransactionRepository;

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String TRANSFER_ENCODING_HEADER = "Content-transfer-encoding";
    private static final String TRANSFER_ENCODING_TEXT = "text";

    // Timeout configurations
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int READ_TIMEOUT_SECONDS = 60;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    @Override
    public YoPaymentRepaymentTransaction processRepayment(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction)
            throws IOException {
        // Check if Yo Payment Mobile Money is enabled
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_YO_PAYMENT_MOBILE_MONEY_PAYMENT);

        if (!property.isEnabled()) {
            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.payments.is.disabled",
                    "Yo Payment Mobile Money payments is disabled");
        }

        // Create a transaction record
        YoPaymentRepaymentTransaction yoTransaction = YoPaymentRepaymentTransaction.builder().loan(loan).loanTransaction(loanTransaction)
                .middlewareReferenceNo(null).dateOf(loanTransaction.getTransactionDate()).amount(loanTransaction.getAmount())
                .reversed(false).statusCode(null).statusDesc("PENDING").vendorTranId(momoPaymentData.getVendorTranId()).build();

        // Create the Yo Payment request for deposit (repayment)
        YoPaymentRequest yoRequest = createYoPaymentDepositRequest(momoPaymentData);

        // Convert the request to XML
        String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
        yoTransaction.setRequestBody(xmlRequest);
        log.debug("Yo Payment Repayment XML Request: {}", xmlRequest);

        // Send the request to Yo Payment API with retry logic
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
        String url = urlBuilder.build().toString();

        // Create OkHttpClient with timeout configuration
        OkHttpClient client = createTrustAllClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);

        Request request = new Request.Builder().url(url).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT).post(requestBody).build();

        // Implement retry logic
        int retryCount = 0;
        IOException lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                Response response = client.newCall(request).execute();
                String xmlResponse = response.body().string();
                yoTransaction.setResponseBody(xmlResponse);
                log.debug("Yo Payment Repayment XML Response: {}", xmlResponse);

                // Parse the response
                YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);

                if (response.isSuccessful() && "OK".equals(yoResponse.getStatus())
                        && ("PENDING".equals(yoResponse.getTransactionStatus()) || "SUCCEEDED".equals(yoResponse.getTransactionStatus()))) {

                    // Transaction is in PENDING or SUCCEEDED state
                    log.info("Yo Payment Repayment Response: {}", yoResponse);

                    yoTransaction.setMiddlewareReferenceNo(yoResponse.getTransactionReference());
                    yoTransaction.setStatusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null);
                    yoTransaction.setStatusDesc(yoResponse.getTransactionStatus());

                    // Save the transaction record
                    yoPaymentRepaymentTransactionRepository.save(yoTransaction);

                    return yoTransaction;
                } else {
                    // API call failed
                    log.error("Yo Payment Repayment API call failed: {}", xmlResponse);

                    yoTransaction.setStatusCode(response.code() + "");
                    yoTransaction.setStatusDesc("FAILED");
                    yoTransaction.setErrorMsg(yoResponse.getErrorMessage());

                    // Save the transaction record
                    yoPaymentRepaymentTransactionRepository.save(yoTransaction);

                    String errorMessage = yoResponse.getErrorMessage() != null ? yoResponse.getErrorMessage() : "Unknown error occurred";
                    throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.repayment.api.failed",
                            "Failed to process Yo Payment repayment: " + errorMessage);
                }
            } catch (IOException e) {
                lastException = e;
                retryCount++;

                if (retryCount < MAX_RETRIES) {
                    log.warn("Yo Payment API call failed (attempt {}/{}). Retrying in {} ms...", retryCount, MAX_RETRIES, RETRY_DELAY_MS);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                } else {
                    log.error("Yo Payment API call failed after {} attempts", MAX_RETRIES, e);

                    // Update transaction with error details
                    yoTransaction.setStatusCode("TIMEOUT");
                    yoTransaction.setStatusDesc("FAILED");
                    yoTransaction.setErrorMsg("API call timed out after " + MAX_RETRIES + " attempts: " + e.getMessage());
                    yoPaymentRepaymentTransactionRepository.save(yoTransaction);

                    throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.repayment.api.timeout",
                            "Yo Payment API call timed out after " + MAX_RETRIES + " attempts. Please try again later.");
                }
            }
        }

        throw lastException;
    }

    @Override
    public YoPaymentRepaymentTransaction checkRepaymentStatus(String transactionReference) throws IOException {
        // Find the transaction record
        YoPaymentRepaymentTransaction transaction = yoPaymentRepaymentTransactionRepository
                .findByMiddlewareReferenceNo(transactionReference)
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.yo.payment.transaction.not.found",
                        "Yo Payment transaction not found with reference: " + transactionReference));

        // For completed transactions, just return the existing record
        if ("SUCCEEDED".equals(transaction.getStatusDesc()) || "FAILED".equals(transaction.getStatusDesc())) {
            return transaction;
        }

        // Create the Yo Payment request to check transaction status
        YoPaymentRequest yoRequest = createYoPaymentStatusRequest(transactionReference);

        // Convert the request to XML
        String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
        log.debug("Yo Payment Status Check XML Request: {}", xmlRequest);

        // Send the request to Yo Payment API
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = createTrustAllClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);

        Request request = new Request.Builder().url(url).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT).post(requestBody).build();

        Response response = client.newCall(request).execute();
        String xmlResponse = response.body().string();
        log.debug("Yo Payment Status Check XML Response: {}", xmlResponse);

        // Parse the response
        YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);

        if (response.isSuccessful() && "OK".equals(yoResponse.getStatus())) {
            // Update the transaction status
            transaction.setStatusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null);
            transaction.setStatusDesc(yoResponse.getTransactionStatus());

            // Save the updated transaction record
            yoPaymentRepaymentTransactionRepository.save(transaction);

            return transaction;
        } else {
            // API call failed
            log.error("Yo Payment Status Check API call failed: {}", xmlResponse);

            String errorMessage = yoResponse.getErrorMessage() != null ? yoResponse.getErrorMessage() : "Unknown error occurred";
            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.status.check.api.failed",
                    "Failed to check Yo Payment status: " + errorMessage);
        }
    }

    /**
     * Creates a Yo Payment deposit (repayment) request
     */
    private YoPaymentRequest createYoPaymentDepositRequest(MomoPaymentData momoPaymentData) {
        String apiUsername = getConfigProperty("yo.payment.api.username");
        String apiPassword = getConfigProperty("yo.payment.api.password");

        // Log the credentials being used (username only for security)
        log.info("Using YO Payment API credentials - Username: {}", apiUsername);

        String account = momoPaymentData.getAccountNumber();
        // Ensure account number is not null or empty
        if (account == null || account.trim().isEmpty()) {
            log.error("Client mobile number is missing or invalid: {}", account);
            throw new GeneralPlatformDomainRuleException("error.msg.client.mobile.number.missing",
                    "Client mobile number is required for mobile money payments");
        }

        BigDecimal amount = momoPaymentData.getTranAmount();
        String narrative = momoPaymentData.getTranNarration();
        String externalReference = momoPaymentData.getVendorTranId();

        // If vendorTranId is null, set it to a unique value
        if (externalReference == null || externalReference.trim().isEmpty()) {
            externalReference = "LOAN-REPAY-" + UUID.randomUUID().toString().substring(0, 8);
            momoPaymentData.setVendorTranId(externalReference);
        }

        // Create a request for deposit funds (acdepositfunds)
        YoPaymentRequest request = new YoPaymentRequest();
        request.setApiUsername(apiUsername);
        request.setApiPassword(apiPassword);
        request.setMethod("acdepositfunds");
        request.setNonBlocking("FALSE");
        request.setAmount(amount);
        request.setAccount(account);
        request.setNarrative(narrative);
        request.setExternalReference(externalReference);

        // Set account provider code if configured
        String providerCode = getConfigProperty("yo.payment.provider.code");
        if (providerCode != null && !providerCode.trim().isEmpty()) {
            request.setProviderCode(providerCode);
        }

        return request;
    }

    /**
     * Creates a Yo Payment transaction status check request
     */
    private YoPaymentRequest createYoPaymentStatusRequest(String transactionReference) {
        String apiUsername = getConfigProperty("yo.payment.api.username");
        String apiPassword = getConfigProperty("yo.payment.api.password");

        // Create a request to check transaction status
        YoPaymentRequest request = new YoPaymentRequest();
        request.setApiUsername(apiUsername);
        request.setApiPassword(apiPassword);
        request.setMethod("actransactioncheckstatus");
        request.setTransactionReference(transactionReference);

        return request;
    }

    /**
     * Creates an OkHttpClient with proper timeout configuration
     */
    private OkHttpClient createTrustAllClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient.Builder().connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create SSL context", e);
        }
    }

    /**
     * Gets a configuration property from environment
     */
    private String getConfigProperty(String key) {
        return env.getProperty(key);
    }
}
