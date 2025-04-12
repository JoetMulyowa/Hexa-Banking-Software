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
package org.apache.fineract.notification.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
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
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.notification.data.SmsNotificationData;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

@RequiredArgsConstructor
@Slf4j
public class SMSNotificationWritePlatformServiceImpl implements SmsNotificationWritePlatformService {

    @Autowired
    private Environment env;
    @Autowired
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    public static final String FORM_URL_CONTENT_TYPE = "application/json";

    @Override
    public void sendSms(SmsNotificationData smsNotificationData) {

        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS);

        if (property.isEnabled() && property.getStringValue() != null) {
            Gson gson = new GsonBuilder().create();

            smsNotificationData.setSender(property.getStringValue());
            smsNotificationData.setService(getConfigProperty("sms.service"));
            smsNotificationData.setPassword(getConfigProperty("sms.password"));

            String notificationObj = gson.toJson(smsNotificationData);
            log.info("SMS Message is constructed :=> " + notificationObj);

            HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("sms.url")).newBuilder();
            String url = urlBuilder.build().toString();

            log.info("SMS URL :=>" + url);
            OkHttpClient client = new OkHttpClient();
            Response response = null;

            RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), notificationObj);

            Request request = new Request.Builder().url(url).post(formBody).build();

            List<Throwable> exceptions = new ArrayList<>();

            try {
                response = client.newCall(request).execute();
                String resObject = response.body().string();
                if (response.isSuccessful()) {

                    log.info("Sms Message Response :=>" + resObject);

                } else {
                    log.error("Failed to deliver sms message notification :" + resObject);

                    handleAPIIntegrityIssues(resObject);

                }
            } catch (Exception e) {
                log.error("Posting sms notification has failed " + e);
                exceptions.add(e);
            }
        } else {
            log.info("** SMS Notification is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
        }
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    private void handleAPIIntegrityIssues(String httpResponse) {
        throw new PlatformDataIntegrityException(httpResponse, httpResponse);
    }

    @Override
    public void processSmsNotification(Loan loan, SmsTypeEnum smsType, LoanTransaction transaction) {

        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS);

        if (!property.isEnabled()) {
            log.info("** SMS Notification is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
            return;
        }

        String clientName = loan.client().getDisplayName();
        String mobileNo = loan.client().getMobileNo();
        String message = null;
        String messageId = null;
        switch (smsType) {
            case LOAN_SUBMISSION:
                message = String.format(
                        "Dear %s, your loan application has been received, it\\'s under review, we will notify you once the process is done, thank you for choosing %s .",
                        clientName, ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-SUBMISSION-%s", loan.getId());
            break;
            case LOAN_APPROVAL:
                message = String.format(
                        "Dear %s, congratulations, your loan %s of %s %s has been approved, please visit our branch for signoff, thank you for choosing %s .",
                        clientName, loan.getId(), loan.getCurrencyCode(), loan.getApprovedPrincipal(),
                        ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-APPROVAL-%s", loan.getId());
            break;
            case LOAN_DISBURSEMENT:
                message = String.format("Dear %s, your %s of %s %s has been disbursed on account number %s, thank you for choosing %s .",
                        clientName, loan.getId(), loan.getCurrencyCode(), loan.getDisbursedAmount(), loan.getAccountNumber(),
                        ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-DISBURSEMENT-%s", loan.getId());
            break;
            case LOAN_REJECTED:
                message = String.format(
                        "Dear %s, your loan application %s of %s %s has been rejected, please reach out to %s nearest branch for more details.",
                        clientName, loan.getId(), loan.getCurrencyCode(), loan.getProposedPrincipal(),
                        ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-REJECTED-%s", loan.getId());
            break;
            case LOAN_REPAYMENT:
                message = String.format(
                        "Dear %s, we have successfully received your loan instalment payment of %s  %s, thank you for banking with %s .",
                        clientName, loan.getCurrencyCode(), transaction.getAmount(), ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-REPAYMENT-%s", transaction.getId());
            break;
            case LOAN_CLOSED:
                message = String.format("Congrats, %s ! You have fully paid off your loan. Thank you for your payments and choosing %s .",
                        clientName, ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-CLOSED-%s", transaction.getId());
            break;
            default:
                log.info("No sms type found to process a notification");
                return;

        }

        if (mobileNo != null) {
            sendSms(new SmsNotificationData(mobileNo, message, messageId));
        }
    }

}
