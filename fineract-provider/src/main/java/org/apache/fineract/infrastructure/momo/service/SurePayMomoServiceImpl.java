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
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Implementation of SurePayMomoService for handling SurePay mobile money transactions
 */
@Service
@Slf4j
public class SurePayMomoServiceImpl implements SurePayMomoService {

    private final Environment env;
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository;

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Autowired
    public SurePayMomoServiceImpl(Environment env, GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository) {
        this.env = env;
        this.configurationRepositoryWrapper = configurationRepositoryWrapper;
        this.loanPaymentTransactionRepository = loanPaymentTransactionRepository;
    }

    @Override
    public void handle(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoTransaction) throws IOException {
        // Check if SurePay Mobile Money is enabled
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SURE_MOBILE_MONEY_PAYMENT);

        if (!property.isEnabled()) {
            throw new GeneralPlatformDomainRuleException("error.msg.sure.momo.payments.is.disabled",
                    "SurePay Mobile Money payments is disabled");
        }

        // Build the SurePay payment request
        String jsonRequest = buildSurePayRequest(momoPaymentData);
        log.debug("SurePay Payment JSON Request: {}", jsonRequest);

        // Send the request to SurePay API
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("surepay.api.url")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_JSON), jsonRequest);

        String apiKey = getConfigProperty("surepay.api.key");

        Request request = new Request.Builder().url(url).header(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON)
                .header(AUTHORIZATION_HEADER, "Bearer " + apiKey).post(requestBody).build();

        Response response = client.newCall(request).execute();
        String jsonResponse = response.body().string();
        log.debug("SurePay Payment JSON Response: {}", jsonResponse);

        if (response.isSuccessful()) {
            // Transaction was accepted by the API
            log.info("SurePay Payment request successful: {}", response.code());
            momoTransaction.setMiddlewareReferenceNo(generateReferenceNumber(momoPaymentData));
            momoTransaction.setStatusCode(response.code() + "");
            momoTransaction.setStatusDesc("PENDING");
            momoTransaction.setResponseBody(jsonResponse);

            loanPaymentTransactionRepository.saveAndFlush(momoTransaction);
        } else {
            // API call failed
            log.error("SurePay Payment API call failed: {} - {}", response.code(), jsonResponse);
            momoTransaction.setStatusCode(response.code() + "");
            momoTransaction.setStatusDesc("FAILED");
            momoTransaction.setResponseBody(jsonResponse);

            loanPaymentTransactionRepository.saveAndFlush(momoTransaction);

            throw new GeneralPlatformDomainRuleException("error.msg.sure.momo.integration.api.failed",
                    "Failed to call SurePay Momo API: " + response.message());
        }
    }

    /**
     * Builds the JSON request for SurePay API
     */
    private String buildSurePayRequest(MomoPaymentData momoPaymentData) {
        String accountNumber = momoPaymentData.getAccountNumber();
        // Ensure account number is not null or empty
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            log.error("Client mobile number is missing or invalid: {}", accountNumber);
            throw new GeneralPlatformDomainRuleException("error.msg.client.mobile.number.missing",
                    "Client mobile number is required for mobile money payments");
        }

        BigDecimal amount = momoPaymentData.getTranAmount();
        String narrative = momoPaymentData.getTranNarration();
        String referenceNumber = generateReferenceNumber(momoPaymentData);

        return String.format("{\"phoneNumber\":\"%s\",\"amount\":%s,\"currency\":\"UGX\",\"description\":\"%s\",\"reference\":\"%s\"}",
                accountNumber, amount.toString(), narrative, referenceNumber);
    }

    /**
     * Generates a reference number for the transaction
     */
    private String generateReferenceNumber(MomoPaymentData momoPaymentData) {
        return "SUREPAY-" + momoPaymentData.getVendorTranId() + "-" + System.currentTimeMillis();
    }

    /**
     * Gets a configuration property from the environment
     */
    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }
}
