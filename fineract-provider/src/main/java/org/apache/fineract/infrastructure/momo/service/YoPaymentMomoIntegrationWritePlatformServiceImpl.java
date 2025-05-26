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
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.util.YoPaymentXmlUtil;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementation of Yo Payment Mobile Money Integration service
 */
@Service
@Slf4j
public class YoPaymentMomoIntegrationWritePlatformServiceImpl extends MomoPaymentIntegrationWritePlatformServiceImpl
        implements YoPaymentMomoIntegrationWritePlatformService {

    private final Environment env;
    private final YoPaymentXmlUtil xmlUtil;
    private final YoPaymentMomoService momoService;
    private final YoPaymentTransactionRepository yoPaymentTransactionRepository;

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String TRANSFER_ENCODING_HEADER = "Content-transfer-encoding";
    private static final String TRANSFER_ENCODING_TEXT = "text";

    @Autowired
    public YoPaymentMomoIntegrationWritePlatformServiceImpl(GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            LoanRepositoryWrapper loanRepositoryWrapper, MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository,
            Environment env, YoPaymentXmlUtil xmlUtil, YoPaymentMomoService momoService,
            YoPaymentTransactionRepository yoPaymentTransactionRepository) {
        super(configurationRepositoryWrapper, loanRepositoryWrapper, loanPaymentTransactionRepository);
        this.env = env;
        this.xmlUtil = xmlUtil;
        this.momoService = momoService;
        this.yoPaymentTransactionRepository = yoPaymentTransactionRepository;
    }

    @Override
    protected void processPayment(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoLoanPaymentTransaction)
            throws IOException {
        // No-op for YoPayment - this method is called from the parent class but we handle everything in our payOut
        // method
        // We don't want to use the generic MomoLoanPaymentTransaction for YoPayment
        log.info("Skipping generic MOMO processing for YO Payment transaction");
    }

    @Override
    protected boolean isEnabled() {
        return configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_YO_PAYMENT_MOBILE_MONEY_PAYMENT).isEnabled();
    }

    @Override
    public String getPaymentTypeCode() {
        return "YO_PAYMENT_MOMO_PAYMENT";
    }

    @Override
    public void payOut(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction) throws IOException {
        // Check if Yo Payment Mobile Money is enabled
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_YO_PAYMENT_MOBILE_MONEY_PAYMENT);

        if (!property.isEnabled()) {
            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.payments.is.disabled",
                    "Yo Payment Mobile Money payments is disabled");
        }

        // Create the Yo Payment request
        YoPaymentRequest yoRequest = createYoPaymentRequest(momoPaymentData);

        // Convert the request to XML
        String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
        log.debug("Yo Payment XML Request: {}", xmlRequest);

        // Send the request to Yo Payment API
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
        String url = urlBuilder.build().toString();

        // Create a trust manager that does not validate certificate chains
        OkHttpClient client = createTrustAllClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);

        Request request = new Request.Builder().url(url).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT).post(requestBody).build();

        Response response = client.newCall(request).execute();
        String xmlResponse = response.body().string();
        log.debug("Yo Payment XML Response: {}", xmlResponse);

        // Parse the response
        YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);

        if (response.isSuccessful() && "OK".equals(yoResponse.getStatus())
                && ("PENDING".equals(yoResponse.getTransactionStatus()) || "SUCCEEDED".equals(yoResponse.getTransactionStatus()))) {

            // Transaction is in PENDING or SUCCEEDED state, update loan and create transaction record
            log.info("Yo Payment Response: {}", yoResponse);

            loan.setDisbursedViaMomoPay(Boolean.TRUE);
            loan.setDisbursementPayoutCompleted(Boolean.TRUE);
            loan.setMiddlewareReferenceNo(yoResponse.getTransactionReference());
            this.loanRepositoryWrapper.saveAndFlush(loan);

            String statusDesc = yoResponse.getTransactionStatus();

            // Skip saving to MomoLoanPaymentTransaction for YO payments
            // Only create a YoPaymentTransaction record
            YoPaymentTransaction yoTransaction = YoPaymentTransaction.builder().loan(loan).loanTransaction(loanTransaction)
                    .middlewareReferenceNo(yoResponse.getTransactionReference()).dateOf(loanTransaction.getTransactionDate())
                    .amount(loanTransaction.getAmount()).reversed(false)
                    .statusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null).statusDesc(statusDesc)
                    .requestBody(xmlRequest).responseBody(xmlResponse).vendorTranId(momoPaymentData.getVendorTranId()).build();

            this.yoPaymentTransactionRepository.save(yoTransaction);

        } else {
            log.error("Yo Payment API returned an error: {}", yoResponse);

            // Create a failed YoPaymentTransaction record
            YoPaymentTransaction yoTransaction = YoPaymentTransaction.builder().loan(loan).loanTransaction(loanTransaction)
                    .dateOf(loanTransaction.getTransactionDate()).amount(loanTransaction.getAmount()).reversed(false)
                    .statusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null).statusDesc("FAILED")
                    .requestBody(xmlRequest).responseBody(xmlResponse).errorMsg(yoResponse.getErrorMessage())
                    .vendorTranId(momoPaymentData.getVendorTranId()).build();

            this.yoPaymentTransactionRepository.save(yoTransaction);

            String errorMessage = yoResponse.getErrorMessage() != null ? yoResponse.getErrorMessage() : "Unknown error occurred";

            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.integration.failed",
                    "Failed to process Yo Payment Momo transaction: " + errorMessage);
        }
    }

    /**
     * Creates a Yo Payment request from Momo payment data
     */
    private YoPaymentRequest createYoPaymentRequest(MomoPaymentData momoPaymentData) {
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

        // If vendorTranId is null, set it to the narrative
        if (externalReference == null || externalReference.trim().isEmpty()) {
            externalReference = narrative;
            momoPaymentData.setVendorTranId(externalReference);
        }

        YoPaymentRequest request = new YoPaymentRequest(apiUsername, apiPassword, account, amount, narrative, externalReference);

        // Set account provider code if configured
        String accountProviderCode = getConfigProperty("yo.payment.provider.code");
        if (accountProviderCode != null && !accountProviderCode.isEmpty()) {
            request.setAccountProviderCode(accountProviderCode);
        }

        return request;
    }

    /**
     * Gets a configuration property from environment
     */
    private String getConfigProperty(String key) {
        return env.getProperty(key);
    }

    /**
     * Creates an OkHttpClient that trusts all certificates This is only for development/testing purposes
     */
    private OkHttpClient createTrustAllClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] { new javax.net.ssl.X509TrustManager() {

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
            } };

            // Install the all-trusting trust manager
            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            // Add timeouts to prevent hanging connections
            builder.connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.readTimeout(30, java.util.concurrent.TimeUnit.SECONDS);
            builder.writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            log.error("Error creating trust all client", e);
            return new OkHttpClient();
        }
    }
}
