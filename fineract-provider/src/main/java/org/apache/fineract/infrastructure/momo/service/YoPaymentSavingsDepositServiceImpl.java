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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.infrastructure.momo.data.YoPaymentRequest;
import org.apache.fineract.infrastructure.momo.data.YoPaymentResponse;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentSavingsDepositTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentSavingsDepositTransactionRepository;
import org.apache.fineract.infrastructure.momo.util.YoPaymentXmlUtil;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.service.SavingsAccountDomainService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the YoPaymentSavingsDepositService for processing deposits via Yo Payment
 */
@Service
@Slf4j
public class YoPaymentSavingsDepositServiceImpl implements YoPaymentSavingsDepositService {

    private final Environment env;
    private final YoPaymentSavingsDepositTransactionRepository transactionRepository;
    private final SavingsAccountDomainService savingsAccountDomainService;
    
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String TRANSFER_ENCODING_HEADER = "Content-transfer-encoding";
    private static final String TRANSFER_ENCODING_TEXT = "text";
    
    @Autowired
    public YoPaymentSavingsDepositServiceImpl(Environment env, 
            YoPaymentSavingsDepositTransactionRepository transactionRepository,
            SavingsAccountDomainService savingsAccountDomainService) {
        this.env = env;
        this.transactionRepository = transactionRepository;
        this.savingsAccountDomainService = savingsAccountDomainService;
    }
    
    @Override
    @Transactional
    public YoPaymentSavingsDepositTransaction processDeposit(MomoPaymentData momoPaymentData, SavingsAccount savingsAccount) 
            throws IOException {
        
        // Create a transaction record to track the deposit request
        YoPaymentSavingsDepositTransaction transaction = YoPaymentSavingsDepositTransaction.create(
                savingsAccount, 
                momoPaymentData.getAmount(), 
                momoPaymentData.getMobileNumber(),
                LocalDateTime.now());
        
        // Save the transaction record
        transaction = transactionRepository.save(transaction);
        
        try {
            // Create the Yo Payment request
            YoPaymentRequest yoRequest = createYoPaymentDepositRequest(momoPaymentData, transaction);
            
            // Convert the request to XML
            String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
            log.debug("Yo Payment Savings Deposit XML Request: {}", xmlRequest);
            
            // Update transaction with request details
            transaction.updateRequest(xmlRequest);
            transactionRepository.save(transaction);
            
            // Send the request to Yo Payment API
            HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
            String url = urlBuilder.build().toString();
            
            // Create a trust manager that does not validate certificate chains
            OkHttpClient client = createTrustAllClient();
            RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);
            
            Request request = new Request.Builder()
                    .url(url)
                    .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                    .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT)
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String xmlResponse = response.body().string();
            log.debug("Yo Payment Savings Deposit XML Response: {}", xmlResponse);
            
            // Parse the response
            YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);
            
            // Update transaction with response details
            transaction.updateResponse(
                    xmlResponse, 
                    yoResponse.getTransactionReference(),
                    String.valueOf(yoResponse.getStatusCode()),
                    yoResponse.getStatus());
            
            // If transaction was successful, complete it immediately
            if (isSuccess(yoResponse) && "SUCCEEDED".equals(yoResponse.getTransactionStatus())) {
                // Complete the deposit
                SavingsAccountTransaction savingsTransaction = completeDeposit(transaction);
                transaction.linkSavingsTransaction(savingsTransaction);
            } else if (isSuccess(yoResponse) && "PENDING".equals(yoResponse.getTransactionStatus())) {
                // Transaction is pending, will be processed when status is checked
                log.info("Savings deposit transaction is pending for account {}, transaction reference: {}",
                        savingsAccount.getId(), yoResponse.getTransactionReference());
            } else {
                // Transaction failed
                log.error("Savings deposit transaction failed for account {}: {}",
                        savingsAccount.getId(), yoResponse.getStatusMessage());
                transaction.updateError(yoResponse.getStatusMessage());
            }
            
            // Save updated transaction
            transaction = transactionRepository.save(transaction);
            
            return transaction;
        } catch (Exception e) {
            log.error("Error processing savings deposit via Yo Payment for account {}: {}",
                    savingsAccount.getId(), e.getMessage(), e);
            transaction.updateError(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public boolean checkDepositStatus(String transactionReference) throws IOException {
        // Find the transaction record
        Optional<YoPaymentSavingsDepositTransaction> transactionOpt = 
                transactionRepository.findByVendorTransactionId(transactionReference);
        
        if (!transactionOpt.isPresent()) {
            log.warn("No savings deposit transaction found with reference: {}", transactionReference);
            return false;
        }
        
        YoPaymentSavingsDepositTransaction transaction = transactionOpt.get();
        
        // Skip if the transaction is already linked to a savings transaction
        if (transaction.getSavingsTransaction() != null) {
            log.debug("Savings deposit transaction already processed for reference: {}", transactionReference);
            return true;
        }
        
        try {
            // Create a status check request
            YoPaymentRequest yoRequest = createYoPaymentStatusRequest(transactionReference);
            
            // Convert the request to XML
            String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
            log.debug("Yo Payment Status Check XML Request: {}", xmlRequest);
            
            // Send the request to Yo Payment API
            HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
            String url = urlBuilder.build().toString();
            
            OkHttpClient client = createTrustAllClient();
            RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);
            
            Request request = new Request.Builder()
                    .url(url)
                    .header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                    .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT)
                    .post(requestBody)
                    .build();
            
            Response response = client.newCall(request).execute();
            String xmlResponse = response.body().string();
            log.debug("Yo Payment Status Check XML Response: {}", xmlResponse);
            
            // Parse the response
            YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);
            
            // Update transaction status
            transaction.updateResponse(
                    xmlResponse, 
                    transactionReference,
                    String.valueOf(yoResponse.getStatusCode()),
                    yoResponse.getStatus());
            
            // If transaction was successful, complete it
            if (isSuccess(yoResponse) && "SUCCEEDED".equals(yoResponse.getTransactionStatus())) {
                // Complete the deposit
                SavingsAccountTransaction savingsTransaction = completeDeposit(transaction);
                transaction.linkSavingsTransaction(savingsTransaction);
                transactionRepository.save(transaction);
                return true;
            } else if (isSuccess(yoResponse) && "PENDING".equals(yoResponse.getTransactionStatus())) {
                // Transaction is still pending
                log.info("Savings deposit transaction is still pending for reference: {}", transactionReference);
                transactionRepository.save(transaction);
                return false;
            } else {
                // Transaction failed
                log.error("Savings deposit transaction failed for reference {}: {}",
                        transactionReference, yoResponse.getStatusMessage());
                transaction.updateError(yoResponse.getStatusMessage());
                transactionRepository.save(transaction);
                return false;
            }
        } catch (Exception e) {
            log.error("Error checking savings deposit status for reference {}: {}",
                    transactionReference, e.getMessage(), e);
            transaction.updateError(e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public SavingsAccountTransaction completeDeposit(YoPaymentSavingsDepositTransaction depositTransaction) {
        log.info("Completing savings deposit for account: {} with amount: {}",
                depositTransaction.getSavingsAccount().getId(), depositTransaction.getAmount());
        
        // Format for the transaction date
        final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        final LocalDateTime transactionDate = depositTransaction.getTransactionDate();
        
        // Create a deposit transaction on the savings account
        SavingsAccountTransaction transaction = savingsAccountDomainService.handleDeposit(
                depositTransaction.getSavingsAccount(),
                fmt,
                transactionDate.toLocalDate(),
                depositTransaction.getAmount(),
                null, // paymentDetail
                false, // isAccountTransfer
                true, // isRegularTransaction
                false  // backdatedTxnsAllowedTill
        );
        
        return transaction;
    }
    
    /**
     * Creates a Yo Payment deposit funds request
     */
    private YoPaymentRequest createYoPaymentDepositRequest(MomoPaymentData momoPaymentData, 
            YoPaymentSavingsDepositTransaction transaction) {
        
        String apiUsername = getConfigProperty("yo.payment.api.username");
        String apiPassword = getConfigProperty("yo.payment.api.password");
        
        // Create a complete deposit request
        YoPaymentRequest request = new YoPaymentRequest();
        request.setApiUsername(apiUsername);
        request.setApiPassword(apiPassword);
        request.setMethod("acdepositfunds");
        request.setNonBlocking("FALSE");
        request.setAmount(momoPaymentData.getAmount());
        request.setAccount(momoPaymentData.getMobileNumber());
        
        // Set narrative with account details
        String narrative = "Deposit to savings account " + transaction.getSavingsAccount().getAccountNumber();
        if (momoPaymentData.getNote() != null && !momoPaymentData.getNote().isEmpty()) {
            narrative += " - " + momoPaymentData.getNote();
        }
        request.setNarrative(narrative);
        
        // Set account provider code if available
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
     * Checks if the Yo Payment response indicates success
     */
    private boolean isSuccess(YoPaymentResponse response) {
        return "OK".equals(response.getStatus()) && (response.getStatusCode() == null || response.getStatusCode() == 0);
    }
    
    /**
     * Creates an OkHttpClient with proper timeout configuration
     */
    private OkHttpClient createTrustAllClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
            
            // Configure timeouts
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            builder.writeTimeout(30, TimeUnit.SECONDS);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Gets a configuration property from the environment
     */
    private String getConfigProperty(String key) {
        return env.getProperty(key);
    }
}
 
