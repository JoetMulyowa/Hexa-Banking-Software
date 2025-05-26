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
package org.apache.fineract.infrastructure.momo.api;

import com.google.gson.JsonElement;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentSavingsDepositTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentSavingsDepositTransactionRepository;
import org.apache.fineract.infrastructure.momo.service.YoPaymentSavingsDepositService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller for handling webhook callbacks from Yo Payment for savings account deposits.
 */
@Path("/v1/yopayment/webhooks")
@Component
@Scope("singleton")
@Slf4j
@RequiredArgsConstructor
public class YoPaymentWebhookController {

    private final YoPaymentSavingsDepositTransactionRepository transactionRepository;
    private final YoPaymentSavingsDepositService yoPaymentSavingsDepositService;
    private final FromJsonHelper fromJsonHelper;
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;

    /**
     * Handles webhook callbacks from Yo Payment for transaction status updates
     * 
     * @param notificationSecret The secret key for verifying the webhook is legitimate
     * @param webhookPayload The JSON payload from Yo Payment
     * @return HTTP response
     */
    @POST
    @Path("savings/deposit")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Transactional
    public Response handleSavingsDepositCallback(
            @HeaderParam("X-YoPayment-Webhook-Secret") String notificationSecret,
            String webhookPayload) {
        
        log.info("Received Yo Payment webhook notification for savings deposit");
        
        try {
            // Verify webhook secret
            String configuredSecret = configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection("FINERACT_YO_PAYMENT_NOTIFICATION_SECRET")
                .getStringValue();
            
            if (StringUtils.isEmpty(configuredSecret) || !configuredSecret.equals(notificationSecret)) {
                log.warn("Invalid webhook secret provided: {}", notificationSecret);
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"status\":\"error\",\"message\":\"Invalid webhook secret\"}").build();
            }
            
            // Parse the webhook payload
            JsonElement jsonElement = fromJsonHelper.parse(webhookPayload);
            String transactionReference = fromJsonHelper.extractStringNamed("transactionReference", jsonElement);
            String status = fromJsonHelper.extractStringNamed("status", jsonElement);
            
            log.info("Processing Yo Payment webhook for transaction: {}, status: {}", transactionReference, status);
            
            if (StringUtils.isEmpty(transactionReference)) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"status\":\"error\",\"message\":\"Missing transaction reference\"}").build();
            }
            
            // Find the pending transaction
            Optional<YoPaymentSavingsDepositTransaction> optionalTransaction = 
                transactionRepository.findByMiddlewareReferenceNo(transactionReference);
            
            if (!optionalTransaction.isPresent() || !"PENDING".equals(optionalTransaction.get().getStatusDescription())) {
                log.warn("Transaction not found or not in PENDING status: {}", transactionReference);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"status\":\"error\",\"message\":\"Transaction not found or not pending\"}").build();
            }
            
            YoPaymentSavingsDepositTransaction transaction = optionalTransaction.get();
            
            // Update transaction status
            transaction.setStatusCode(status);
            
            if ("SUCCESS".equalsIgnoreCase(status)) {
                transaction.setStatusDescription("COMPLETED");
                
                // Create savings transaction to complete the deposit
                SavingsAccountTransaction savingsTransaction = 
                    yoPaymentSavingsDepositService.completeDeposit(transaction);
                
                transaction.setSavingsTransaction(savingsTransaction);
                
                log.info("Successfully completed Yo Payment savings deposit: {}, savings transaction ID: {}",
                    transactionReference, (Object) savingsTransaction.getId());
            } else if ("FAILED".equalsIgnoreCase(status)) {
                transaction.setStatusDescription("FAILED");
                log.info("Yo Payment savings deposit failed: {}", transactionReference);
            }
            
            // Save the updated transaction
            transactionRepository.save(transaction);
            
            return Response.status(Response.Status.OK)
                .entity("{\"status\":\"success\",\"message\":\"Webhook processed successfully\"}").build();
                
        } catch (Exception e) {
            log.error("Error processing Yo Payment webhook notification: {}", e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"status\":\"error\",\"message\":\"Internal server error\"}").build();
        }
    }
} 