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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import org.apache.fineract.notification.domain.SmsOutboundMessage;
import org.apache.fineract.notification.domain.SmsOutboundMessageRepository;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("africasTalkingSmsNotificationWritePlatformService")
@Slf4j
public class AfricasTalkingSmsNotificationWritePlatformServiceImpl implements SmsNotificationWritePlatformService {

    private final Environment env;
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final SmsOutboundMessageRepository smsOutboundMessageRepository;

    public static final String FORM_URL_CONTENT_TYPE = "application/json";

    @Autowired
    public AfricasTalkingSmsNotificationWritePlatformServiceImpl(GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            SmsOutboundMessageRepository smsOutboundMessageRepository, Environment env) {
        this.env = env;
        this.configurationRepositoryWrapper = configurationRepositoryWrapper;
        this.smsOutboundMessageRepository = smsOutboundMessageRepository;
    }

    @Override
    @Transactional
    public void sendSms(SmsNotificationData smsNotificationData) {
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS);

        if (property.isEnabled()) {
            // Create a record in the database first
            SmsOutboundMessage smsMessage = new SmsOutboundMessage(smsNotificationData.getPhoneNumber(), smsNotificationData.getMessage(),
                    smsNotificationData.getReferenceId(), smsNotificationData.getReferenceType(), smsNotificationData.getEntityId());

            // Save the initial message record
            smsOutboundMessageRepository.save(smsMessage);

            try {
                // Create Africa's Talking API request format
                JsonObject requestBody = new JsonObject();

                // Required parameters for Africa's Talking API
                requestBody.addProperty("username", getConfigProperty("africastalking.username"));
                requestBody.addProperty("message", smsNotificationData.getMessage());

                // Phone numbers must be in a JSON array format
                com.google.gson.JsonArray phoneNumbersArray = new com.google.gson.JsonArray();
                phoneNumbersArray.add(smsNotificationData.getPhoneNumber());
                requestBody.add("phoneNumbers", phoneNumbersArray);

                // Optional parameters
                String senderId = getConfigProperty("africastalking.sender.id");
                if (senderId != null && !senderId.isEmpty() && !senderId.equals("AFRICASTKNG")) {
                    // Only add the sender ID if it's not the default value and properly configured
                    requestBody.addProperty("senderId", senderId);
                }

                // Use boolean true instead of integer 1 for enqueue parameter
                requestBody.addProperty("enqueue", true);

                String notificationObj = requestBody.toString();
                log.info("Africa's Talking SMS request constructed: {}", notificationObj);

                // Determine which endpoint to use (live or sandbox)
                String baseUrl = Boolean.parseBoolean(getConfigProperty("africastalking.sandbox"))
                        ? "https://api.sandbox.africastalking.com/version1/messaging/bulk"
                        : "https://api.africastalking.com/version1/messaging/bulk";

                HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
                String url = urlBuilder.build().toString();

                log.info("Africa's Talking SMS URL: {}", url);

                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), notificationObj);

                // Set up request with proper headers
                Request request = new Request.Builder().url(url).post(formBody).addHeader("Accept", "application/json")
                        .addHeader("Content-Type", "application/json").addHeader("ApiKey", getConfigProperty("africastalking.api.key"))
                        .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    log.info("Africa's Talking SMS response: {}", responseBody);

                    // Parse the response to check for API-level errors
                    if (responseBody.contains("InvalidSenderId")) {
                        log.warn("Africa's Talking rejected the sender ID. Make sure it's registered in your Africa's Talking account.");
                        smsMessage.updateWithError("Invalid Sender ID: " + senderId);
                    } else {
                        // Process successful response
                        processSuccessfulResponse(responseBody, smsMessage);
                    }
                } else {
                    log.error("Failed to deliver Africa's Talking SMS message: {}", responseBody);
                    smsMessage.updateWithError(responseBody);
                    handleAPIIntegrityIssues(responseBody);
                }
            } catch (Exception e) {
                log.error("Error sending SMS via Africa's Talking API: {}", e.getMessage(), e);
                smsMessage.updateWithError(e.getMessage());
            } finally {
                // Update the message record in the database
                smsOutboundMessageRepository.save(smsMessage);
            }
        } else {
            log.info("SMS Notification is disabled for this tenant: {}", ThreadLocalContextUtil.getTenant().getName());
        }
    }

    private void processSuccessfulResponse(String responseBody, SmsOutboundMessage smsMessage) {
        try {
            JsonElement jsonElement = JsonParser.parseString(responseBody);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (jsonObject.has("SMSMessageData")) {
                JsonObject smsMessageData = jsonObject.getAsJsonObject("SMSMessageData");

                if (smsMessageData.has("Recipients") && smsMessageData.get("Recipients").isJsonArray()) {
                    JsonArray recipients = smsMessageData.getAsJsonArray("Recipients");

                    if (recipients.size() > 0) {
                        JsonObject recipient = recipients.get(0).getAsJsonObject();

                        String messageId = recipient.has("messageId") ? recipient.get("messageId").getAsString() : null;
                        String status = recipient.has("status") ? recipient.get("status").getAsString() : null;
                        String cost = recipient.has("cost") ? recipient.get("cost").getAsString() : null;
                        int messageParts = recipient.has("messageParts") ? recipient.get("messageParts").getAsInt() : 1;

                        // Update the SMS record with delivery details
                        smsMessage.updateDeliveryStatus(messageId, status, cost, messageParts);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Africa's Talking response: {}", e.getMessage(), e);
            smsMessage.updateWithError("Error parsing response: " + e.getMessage());
        }
    }

    private String getConfigProperty(String propertyName) {
        if (this.env != null) {
            return this.env.getProperty(propertyName);
        }
        // Return default values if environment is not available
        if ("africastalking.username".equals(propertyName)) {
            return "flexiblexerp";
        } else if ("africastalking.api.key".equals(propertyName)) {
            return "atsk_41f5a518fe35133bc9be8afb6cabc04b7b9e25ac7aabf4f42a18152e2dc704890d02018e";
        } else if ("africastalking.sender.id".equals(propertyName)) {
            return "FINERACT";
        } else if ("africastalking.sandbox".equals(propertyName)) {
            return "false";
        }
        return null;
    }

    private void handleAPIIntegrityIssues(String httpResponse) {
        throw new PlatformDataIntegrityException(httpResponse, httpResponse);
    }

    @Override
    @Transactional
    public void processSmsNotification(Loan loan, SmsTypeEnum smsType, LoanTransaction transaction) {
        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS);

        if (!property.isEnabled()) {
            log.info("SMS Notification is disabled for this tenant: {}", ThreadLocalContextUtil.getTenant().getName());
            return;
        }

        String clientName = loan.client().getDisplayName();
        String mobileNo = loan.client().getMobileNo();
        String message = null;
        String messageId = null;

        switch (smsType) {
            case LOAN_SUBMISSION:
                message = String.format(
                        "Dear %s, your loan application has been received, it's under review, we will notify you once the process is done, thank you for choosing %s.",
                        clientName, ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-SUBMISSION-%s", loan.getId());
            break;
            case LOAN_APPROVAL:
                message = String.format(
                        "Dear %s, congratulations, your loan %s of %s %s has been approved, please visit our branch for signoff, thank you for choosing %s.",
                        clientName, loan.getId(), loan.getCurrencyCode(), loan.getApprovedPrincipal(),
                        ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-APPROVAL-%s", loan.getId());
            break;
            case LOAN_DISBURSEMENT:
                message = String.format("Dear %s, your %s of %s %s has been disbursed on account number %s, thank you for choosing %s.",
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
                        "Dear %s, we have successfully received your loan instalment payment of %s %s, thank you for banking with %s.",
                        clientName, loan.getCurrencyCode(), transaction.getAmount(), ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-REPAYMENT-%s", transaction.getId());
            break;
            case LOAN_CLOSED:
                message = String.format("Congrats, %s! You have fully paid off your loan. Thank you for your payments and choosing %s.",
                        clientName, ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("LOAN-CLOSED-%s", transaction.getId());
            break;
            default:
                log.info("No sms type found to process a notification");
                return;
        }

        if (mobileNo != null) {
            SmsNotificationData smsData = new SmsNotificationData(mobileNo, message, messageId);
            // Add entity reference information
            smsData.setReferenceType("LOAN");
            smsData.setEntityId(loan.getId());
            smsData.setReferenceId(smsType.name());

            // Send the SMS
            sendSms(smsData);
        }
    }
}
