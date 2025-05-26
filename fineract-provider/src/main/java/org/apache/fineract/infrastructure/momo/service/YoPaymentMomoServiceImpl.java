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
import org.apache.fineract.infrastructure.momo.domain.YoPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.util.YoPaymentXmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementation of YoPaymentMomoService for handling Yo Payment mobile money transactions
 */
@Service
@Slf4j
public class YoPaymentMomoServiceImpl implements YoPaymentMomoService {

    private final Environment env;
    private final YoPaymentXmlUtil xmlUtil;
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final YoPaymentTransactionRepository yoPaymentTransactionRepository;

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_XML = "text/xml";
    private static final String TRANSFER_ENCODING_HEADER = "Content-transfer-encoding";
    private static final String TRANSFER_ENCODING_TEXT = "text";

    @Autowired
    public YoPaymentMomoServiceImpl(Environment env, YoPaymentXmlUtil xmlUtil,
            GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            YoPaymentTransactionRepository yoPaymentTransactionRepository) {
        this.env = env;
        this.xmlUtil = xmlUtil;
        this.configurationRepositoryWrapper = configurationRepositoryWrapper;
        this.yoPaymentTransactionRepository = yoPaymentTransactionRepository;
    }

    @Override
    public void handle(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoTransaction) throws IOException {
        // Check if Yo Payment Mobile Money is enabled
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_YO_PAYMENT_MOBILE_MONEY_PAYMENT);

        if (!property.isEnabled()) {
            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.payments.is.disabled",
                    "Yo Payment Mobile Money payments is disabled");
        }

        // Create a YoPaymentTransaction from the MomoLoanPaymentTransaction
        YoPaymentTransaction yoTransaction = YoPaymentTransaction.builder().loan(momoTransaction.getLoan())
                .loanTransaction(momoTransaction.getLoanTransaction()).dateOf(momoTransaction.getDateOf())
                .amount(momoTransaction.getAmount()).reversed(momoTransaction.getReversed()).vendorTranId(momoTransaction.getVendorTranId())
                .build();

        // Create the Yo Payment request
        YoPaymentRequest yoRequest = createYoPaymentRequest(momoPaymentData);

        // Convert the request to XML
        String xmlRequest = YoPaymentXmlUtil.convertRequestToXml(yoRequest);
        yoTransaction.setRequestBody(xmlRequest);
        log.debug("Yo Payment XML Request: {}", xmlRequest);

        // Send the request to Yo Payment API
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("yo.payment.api.url")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_XML), xmlRequest);

        Request request = new Request.Builder().url(url).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_XML)
                .header(TRANSFER_ENCODING_HEADER, TRANSFER_ENCODING_TEXT).post(requestBody).build();

        Response response = client.newCall(request).execute();
        String xmlResponse = response.body().string();
        yoTransaction.setResponseBody(xmlResponse);
        log.debug("Yo Payment XML Response: {}", xmlResponse);

        // Parse the response
        YoPaymentResponse yoResponse = YoPaymentXmlUtil.parseXmlResponse(xmlResponse);

        if (response.isSuccessful() && "OK".equals(yoResponse.getStatus())) {
            if ("PENDING".equals(yoResponse.getTransactionStatus())) {
                // Transaction is in PENDING state, update transaction record
                log.info("Yo Payment Transaction is in PENDING state: {}", yoResponse);
                yoTransaction.setMiddlewareReferenceNo(yoResponse.getTransactionReference());
                yoTransaction.setStatusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null);
                yoTransaction.setStatusDesc("PENDING");

                yoPaymentTransactionRepository.save(yoTransaction);
            } else if ("SUCCEEDED".equals(yoResponse.getTransactionStatus())) {
                // Transaction succeeded
                log.info("Yo Payment Transaction SUCCEEDED: {}", yoResponse);
                yoTransaction.setMiddlewareReferenceNo(yoResponse.getTransactionReference());
                yoTransaction.setStatusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null);
                yoTransaction.setStatusDesc("SUCCEEDED");

                yoPaymentTransactionRepository.save(yoTransaction);
            } else {
                // Transaction is in other state (FAILED, etc.)
                log.error("Yo Payment Transaction failed with status: {}", yoResponse.getTransactionStatus());
                yoTransaction.setMiddlewareReferenceNo(yoResponse.getTransactionReference());
                yoTransaction.setStatusCode(yoResponse.getStatusCode() != null ? yoResponse.getStatusCode().toString() : null);
                yoTransaction.setStatusDesc(yoResponse.getTransactionStatus());
                yoTransaction.setErrorMsg(yoResponse.getErrorMessage());

                yoPaymentTransactionRepository.save(yoTransaction);

                String errorMessage = yoResponse.getErrorMessage() != null ? yoResponse.getErrorMessage() : "Unknown error occurred";

                throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.integration.failed",
                        "Failed to process Yo Payment Momo transaction: " + errorMessage);
            }
        } else {
            // API call failed
            log.error("Yo Payment API call failed: {}", xmlResponse);
            yoTransaction.setStatusCode(response.code() + "");
            yoTransaction.setStatusDesc("FAILED");
            yoTransaction.setErrorMsg(yoResponse.getErrorMessage());

            yoPaymentTransactionRepository.save(yoTransaction);

            String errorMessage = yoResponse.getErrorMessage() != null ? yoResponse.getErrorMessage() : "Unknown error occurred";

            throw new GeneralPlatformDomainRuleException("error.msg.yo.momo.integration.api.failed",
                    "Failed to call Yo Payment Momo API: " + errorMessage);
        }
    }

    /**
     * Creates a Yo Payment request from Momo payment data
     */
    private YoPaymentRequest createYoPaymentRequest(MomoPaymentData momoPaymentData) {
        String apiUsername = getConfigProperty("yo.payment.api.username");
        String apiPassword = getConfigProperty("yo.payment.api.password");
        String account = momoPaymentData.getAccountNumber();
        BigDecimal amount = momoPaymentData.getTranAmount();
        String narrative = momoPaymentData.getTranNarration();
        String externalReference = momoPaymentData.getVendorTranId();

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
}
