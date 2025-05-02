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
package org.apache.fineract.infrastructure.momo.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Request;
import okhttp3.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentResponse;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProcessLoanTransactionsOnMomoPaymentTasklet implements Tasklet {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessLoanTransactionsOnMomoPaymentTasklet.class);
    public static final String FORM_URL_CONTENT_TYPE = "application/json";

    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository;
    @Autowired
    private Environment env;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOG.info("Started processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());

        List<MomoLoanPaymentTransaction> loanPaymentTransactionList = loanPaymentTransactionRepository.findPendingMomoPayment("PENDING");
        if(!CollectionUtils.isEmpty(loanPaymentTransactionList)){
            for (MomoLoanPaymentTransaction transaction : loanPaymentTransactionList){
                log.info("Transaction :-> "+transaction.getMiddlewareReferenceNo());

                MomoPaymentResponse paymentResponse = getTransactionStatus();
                log.info("Res::----> "+paymentResponse);

            }
        }

        LOG.info("Completed processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());
        return RepeatStatus.FINISHED;
    }

    private MomoPaymentResponse getTransactionStatus() throws IOException {
        MomoPaymentData paymentData = new MomoPaymentData();
        paymentData.setVendorCode(getConfigProperty("momo.vendorCode"));

        Gson gson = new GsonBuilder().create();
        String momo = gson.toJson(paymentData);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("momo.url.payout")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), momo);

        Request request = new Request.Builder().url(url)
                .header("Authorization", encodeBasicAuth(getConfigProperty("momo.username"), getConfigProperty("momo.password")))
                .post(formBody).build();

        response = client.newCall(request).execute();
        String resObject = response.body().string();
        JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();


        MomoPaymentResponse momoPaymentResponse = getMomoResponse(jsonResponse);
        log.info("Status :- >"+momoPaymentResponse.getStatusCode());

        return momoPaymentResponse;
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }
    public String encodeBasicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
    private MomoPaymentResponse getMomoResponse(JsonObject jsonResponse) {
        MomoPaymentResponse momoPaymentResponse = new MomoPaymentResponse();

        momoPaymentResponse.setMsisdn(jsonResponse.get("msisdn").getAsString());
        momoPaymentResponse.setCustomerName(jsonResponse.get("customer_name").getAsString());
        momoPaymentResponse.setTranCode(jsonResponse.get("tran_code").getAsString());
        momoPaymentResponse.setVendorCode(jsonResponse.get("vendor_code").getAsString());
        momoPaymentResponse.setVendorTranId(jsonResponse.get("vendor_tranId").getAsString());
        momoPaymentResponse.setGatewayRef(jsonResponse.get("gateway_ref").getAsString());
        momoPaymentResponse.setFromAccount(jsonResponse.get("from_account").getAsString());
        momoPaymentResponse.setToAccount(jsonResponse.get("to_account").getAsString());
        momoPaymentResponse.setTranType(jsonResponse.get("tran_type").getAsString());
        momoPaymentResponse.setCurrency(jsonResponse.get("currency").getAsString());
        momoPaymentResponse.setAccountType(jsonResponse.get("account_type").getAsString());
        momoPaymentResponse.setRecordDate(jsonResponse.get("record_date").getAsString());
        momoPaymentResponse.setNetwork(jsonResponse.get("network").getAsString());
        momoPaymentResponse.setProcessorId(jsonResponse.get("processor_id").getAsString());
        momoPaymentResponse.setTranNarration(jsonResponse.get("tran_narration").getAsString());
        momoPaymentResponse.setTranStatus(jsonResponse.get("tran_status").getAsString());
        momoPaymentResponse.setReason(jsonResponse.get("reason").getAsString());
        momoPaymentResponse.setTelecomResponseDate(jsonResponse.get("telecom_responsedate").getAsString());
        momoPaymentResponse.setSuspiciousStatus(jsonResponse.get("suspicious_status").getAsString());
        momoPaymentResponse.setReturnUrl(jsonResponse.get("return_url").getAsString());
        momoPaymentResponse.setOvaAffected(jsonResponse.get("ova_affected").getAsString());
        momoPaymentResponse.setTranAmount(jsonResponse.get("tran_amount").getAsString());
        momoPaymentResponse.setTranCharge(jsonResponse.get("tran_charge").getAsString());
        momoPaymentResponse.setConvertedAmount(jsonResponse.get("converted_amount").getAsString());
        return momoPaymentResponse;
    }
}
