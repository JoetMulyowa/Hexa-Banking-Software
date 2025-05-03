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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentResponse;
import org.apache.fineract.infrastructure.momo.data.MomoTransactionTypeEnum;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@Slf4j
@RequiredArgsConstructor
public class ProcessLoanTransactionsOnMomoPaymentTasklet implements Tasklet {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessLoanTransactionsOnMomoPaymentTasklet.class);
    public static final String FORM_URL_CONTENT_TYPE = "application/json";

    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository;
    @Autowired
    private Environment env;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOG.info("Started processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());

        List<MomoLoanPaymentTransaction> loanPaymentTransactionList = loanPaymentTransactionRepository
                .findPendingMomoPayment(MomoTransactionTypeEnum.PENDING.getCode());

        if (!CollectionUtils.isEmpty(loanPaymentTransactionList)) {
            for (MomoLoanPaymentTransaction transaction : loanPaymentTransactionList) {
                log.info("Transaction :-> " + transaction.getMiddlewareReferenceNo());
                String vendorTranId = transaction.getVendorTranId();
                if (vendorTranId != null && !vendorTranId.isEmpty()) {
                    try {
                        MomoPaymentResponse paymentResponse = getTransactionStatus(transaction.getVendorTranId());

                        updateLoanAccountDisbursementDetails(transaction, paymentResponse);
                        updateMomoLoanPaymentTransactionDisbursementDetails(transaction, paymentResponse);
                        // For FAILED Transactions, Reverse the Loan Account back to Approved

                        log.info("Res::----> " + paymentResponse);
                    } catch (IOException e) {
                        // Catch and log API details
                        throw new RuntimeException(e);
                    }
                }

            }
        }

        LOG.info("Completed processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());
        return RepeatStatus.FINISHED;
    }

    private void updateLoanAccountDisbursementDetails(MomoLoanPaymentTransaction transaction, MomoPaymentResponse paymentResponse) {
        Boolean completedTransaction = Boolean.FALSE;
        Loan loan = null;
        loan = transaction.getLoan();

        if (paymentResponse.getTranStatus().equals(MomoTransactionTypeEnum.SUCCESS.getCode())) {
            completedTransaction = Boolean.TRUE;
        }

        loan.setDibursementPayoutCompleted(completedTransaction);
        loan.setDibursementPayoutCompletedDate(DateUtils.parseLocalDateFlexible(paymentResponse.getRecordDate()));
        loanRepositoryWrapper.saveAndFlush(loan);
    }

    private void updateMomoLoanPaymentTransactionDisbursementDetails(MomoLoanPaymentTransaction transaction,
            MomoPaymentResponse paymentResponse) {

        transaction.setStatusCode(paymentResponse.getTranStatus());
        transaction.setStatusDesc(paymentResponse.getTranNarration());
        transaction.setCheckTransactionStatusResponse(paymentResponse.toString());
        loanPaymentTransactionRepository.saveAndFlush(transaction);

    }

    private MomoPaymentResponse getTransactionStatus(String vendorTranId) throws IOException {
        MomoPaymentData paymentData = new MomoPaymentData();
        paymentData.setVendorCode(getConfigProperty("momo.vendorCode"));
        paymentData.setVendorTranId(vendorTranId);

        Gson gson = new GsonBuilder().create();
        String momo = gson.toJson(paymentData);

        HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("momo.checkstatus.url")).newBuilder();
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();
        Response response = null;

        RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), momo);

        Request request = new Request.Builder().url(url).post(formBody).build();

        response = client.newCall(request).execute();
        String resObject = response.body().string();
        JsonObject jsonResponse = JsonParser.parseString(resObject).getAsJsonObject();

        MomoPaymentResponse momoPaymentResponse = getMomoResponse(jsonResponse);

        log.info("Status :- >" + momoPaymentResponse.getStatusCode());

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

        momoPaymentResponse.setMsisdn(getAsStringSafe(jsonResponse, "msisdn"));
        momoPaymentResponse.setCustomerName(getAsStringSafe(jsonResponse, "customer_name"));
        momoPaymentResponse.setTranCode(getAsStringSafe(jsonResponse, "tran_code"));
        momoPaymentResponse.setVendorCode(getAsStringSafe(jsonResponse, "vendor_code"));
        momoPaymentResponse.setVendorTranId(getAsStringSafe(jsonResponse, "vendor_tranId"));
        momoPaymentResponse.setGatewayRef(getAsStringSafe(jsonResponse, "gateway_ref"));
        momoPaymentResponse.setFromAccount(getAsStringSafe(jsonResponse, "from_account"));
        momoPaymentResponse.setToAccount(getAsStringSafe(jsonResponse, "to_account"));
        momoPaymentResponse.setTranType(getAsStringSafe(jsonResponse, "tran_type"));
        momoPaymentResponse.setCurrency(getAsStringSafe(jsonResponse, "currency"));
        momoPaymentResponse.setAccountType(getAsStringSafe(jsonResponse, "account_type"));
        momoPaymentResponse.setRecordDate(getAsStringSafe(jsonResponse, "record_date"));
        momoPaymentResponse.setNetwork(getAsStringSafe(jsonResponse, "network"));
        momoPaymentResponse.setProcessorId(getAsStringSafe(jsonResponse, "processor_id"));
        momoPaymentResponse.setTranNarration(getAsStringSafe(jsonResponse, "tran_narration"));
        momoPaymentResponse.setTranStatus(getAsStringSafe(jsonResponse, "tran_status"));
        momoPaymentResponse.setReason(getAsStringSafe(jsonResponse, "reason"));
        momoPaymentResponse.setTelecomResponseDate(getAsStringSafe(jsonResponse, "telecom_responsedate"));
        momoPaymentResponse.setSuspiciousStatus(getAsStringSafe(jsonResponse, "suspicious_status"));
        momoPaymentResponse.setReturnUrl(getAsStringSafe(jsonResponse, "return_url"));
        momoPaymentResponse.setOvaAffected(getAsStringSafe(jsonResponse, "ova_affected"));
        momoPaymentResponse.setTranAmount(getAsStringSafe(jsonResponse, "tran_amount"));
        momoPaymentResponse.setTranCharge(getAsStringSafe(jsonResponse, "tran_charge"));
        momoPaymentResponse.setConvertedAmount(getAsStringSafe(jsonResponse, "converted_amount"));

        return momoPaymentResponse;
    }

    private String getAsStringSafe(JsonObject obj, String memberName) {
        if (obj.has(memberName) && !obj.get(memberName).isJsonNull()) {
            return obj.get(memberName).getAsString();
        }
        return null;
    }
}
