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
package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.loanaccount.data.MomoPaymentData;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class SurePayMomoPaymentIntegrationWritePlatformServiceImpl implements SurePayMomoPaymentIntegrationWritePlatformService{
    @Autowired
    private Environment env;
    @Autowired
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    public static final String FORM_URL_CONTENT_TYPE = "application/json";
    private static final String HMAC_SHA512 = "HmacSHA512";


    @Override
    public void payOut(MomoPaymentData momoPaymentData) {

        log.info(momoPaymentData.toString());

        String merchantSecretKey = getConfigProperty("momo.secret");
        String message = buildMessage(momoPaymentData);
        String signature = generateSignature(message, merchantSecretKey);

        momoPaymentData.setTranSignature(signature);
        momoPaymentData.setVendorCode(getConfigProperty("momo.vendorCode"));
        momoPaymentData.setTelecom(getConfigProperty("momo.telecom"));

        log.info("Momo Payments - - - >: " + momoPaymentData.toString());
        log.info("Signature: " + signature);

        Gson gson = new GsonBuilder().create();
        String momo = gson.toJson(momoPaymentData);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("momo.url.payout")).newBuilder();
        String url = urlBuilder.build().toString();

        log.info("MOMO URL :=>" + url);
        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), momo);

        Request request = new Request.Builder().url(url).header("Authorization","Basic c3VyZXBheWFkbWluOnNlY3JldDEyMw==").post(formBody).build();

        List<Throwable> exceptions = new ArrayList<>();

        try {
            response = client.newCall(request).execute();
            String resObject = response.body().string();
            if (response.isSuccessful()) {

                log.info("Momo Message Response :=>" + resObject);

            } else {
                log.error("Failed To Make Momo Payout :" + resObject);

                handleAPIIntegrityIssues(resObject);

            }
        } catch (Exception e) {
            log.error("Posting Momo notification has failed " + e);
            exceptions.add(e);
        }

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
        return  accountNumber +accountName+tranAmount+tranType+paymentDate
                +vendorCode+vendorTranId;
    }

    public  String generateSignature(String requestParams, String merchantSecretKey) {
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
    private void handleAPIIntegrityIssues(String httpResponse) {
        throw new PlatformDataIntegrityException(httpResponse, httpResponse);
    }
}
