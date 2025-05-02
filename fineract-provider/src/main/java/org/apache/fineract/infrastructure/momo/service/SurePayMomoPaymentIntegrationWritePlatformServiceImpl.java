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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.loanaccount.data.MomoPaymentData;
import org.apache.fineract.portfolio.loanaccount.data.MomoPaymentResponse;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@RequiredArgsConstructor
@Slf4j
public class SurePayMomoPaymentIntegrationWritePlatformServiceImpl implements SurePayMomoPaymentIntegrationWritePlatformService {

    @Autowired
    private Environment env;
    @Autowired
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    public static final String FORM_URL_CONTENT_TYPE = "application/json";
    private static final String HMAC_SHA512 = "HmacSHA512";
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository;

    @Override
    public void payOut(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction) throws IOException {

        log.info(momoPaymentData.toString());

        String message = buildMessage(momoPaymentData);

        momoPaymentData.setTranSignature(generateSignature(message, getConfigProperty("momo.secret")));
        momoPaymentData.setVendorCode(getConfigProperty("momo.vendorCode"));
        momoPaymentData.setTelecom(getConfigProperty("momo.telecom"));

        log.info("Momo Payments - - - >: " + momoPaymentData.toString());

        Gson gson = new GsonBuilder().create();
        String momo = gson.toJson(momoPaymentData);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("momo.url.payout")).newBuilder();
        String url = urlBuilder.build().toString();

        log.info("MOMO URL :=>" + url);
        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), momo);

        Request request = new Request.Builder().url(url)
                .header("Authorization", encodeBasicAuth(getConfigProperty("momo.username"), getConfigProperty("momo.password")))
                .post(formBody).build();

        response = client.newCall(request).execute();
        String resObject = response.body().string();

        log.info("Momo Message Response :=>" + resObject);
        JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();
        log.info(" Code or status ---- > " + jsonResponse.get("statusCode"));
        if (response.isSuccessful() && jsonResponse.get("statusCode").getAsString().equals("PENDING")) {
            // React to the response from MiddleWare . .
            MomoPaymentResponse resBody = getMomoResponse(jsonResponse);
            log.info("Momo Message Response :=>" + resBody.toString());

            loan.setDisbursedViaMomoPay(Boolean.TRUE);
            loan.setDibursementPayoutCompleted(Boolean.FALSE);
            this.loanRepositoryWrapper.saveAndFlush(loan);

            MomoLoanPaymentTransaction momopay = new MomoLoanPaymentTransaction();

            momopay.setLoan(loan);
            momopay.setLoanTransaction(loanTransaction);
            momopay.setMiddlewareReferenceNo(resBody.getTranReference());
            momopay.setDateOf(loanTransaction.getTransactionDate());
            momopay.setAmount(loanTransaction.getAmount());
            momopay.setReversed(Boolean.FALSE);
            momopay.setTranCharge(BigDecimal.valueOf(Long.parseLong(resBody.getTranCharge())));
            momopay.setStatusCode(resBody.getStatusCode());
            momopay.setStatusDesc(resBody.getStatusDesc());
            momopay.setRequestBody(momo);
            momopay.setResponseBody(resObject);
            loanPaymentTransactionRepository.saveAndFlush(momopay);

        } else {
            log.error("Failed To Make Momo Payout :" + resObject);
            throw new GeneralPlatformDomainRuleException("error.msg.momo.integration.payout.failed",
                    "Failed To Make Momo Payout :" + jsonResponse.get("statusDesc"));
        }

    }

    private MomoPaymentResponse getMomoResponse(JsonObject jsonResponse) {
        MomoPaymentResponse momoPaymentResponse = new MomoPaymentResponse();
        momoPaymentResponse.setVendorTranId(jsonResponse.get("vendorTranId").getAsString());
        momoPaymentResponse.setTranReference(jsonResponse.get("tranReference").getAsString());
        momoPaymentResponse.setStatusCode(jsonResponse.get("statusCode").getAsString());
        momoPaymentResponse.setStatusDesc(jsonResponse.get("statusDesc").getAsString());
        momoPaymentResponse.setProcessorId(jsonResponse.get("processorId").getAsString());
        momoPaymentResponse.setTranCharge(jsonResponse.get("tranCharge").getAsString());
        return momoPaymentResponse;
    }

    @NotNull
    private static String buildMessage(MomoPaymentData momoPaymentData) {
        String accountNumber = momoPaymentData.getAccountNumber();
        String accountName = momoPaymentData.getAccountName();
        String tranType = momoPaymentData.getTranType();
        String tranAmount = momoPaymentData.getTranAmount().toString();
        String paymentDate = momoPaymentData.getPaymentDate().toString();
        String vendorCode = momoPaymentData.getVendorCode();
        String vendorTranId = momoPaymentData.getVendorTranId();
        return accountNumber + accountName + tranAmount + tranType + paymentDate + vendorCode + vendorTranId;
    }

    public String generateSignature(String requestParams, String merchantSecretKey) {
        byte[] secretKeyBytes = merchantSecretKey.getBytes(StandardCharsets.UTF_8);
        Mac hmacSha512 = null;
        try {
            hmacSha512 = Mac.getInstance(HMAC_SHA512);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA512);
        try {
            hmacSha512.init(secretKeySpec);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] macData = hmacSha512.doFinal(requestParams.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(macData);
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    public String encodeBasicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void processLoanTransactionsOnMomoPayment() {

    }
}
