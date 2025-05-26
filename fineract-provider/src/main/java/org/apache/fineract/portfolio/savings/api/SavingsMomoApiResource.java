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
package org.apache.fineract.portfolio.savings.api;

import com.google.gson.JsonElement;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.infrastructure.momo.service.YoPaymentSavingsDepositIntegrationService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.savings.SavingsApiConstants;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotFoundException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Resource for Mobile Money operations on savings accounts
 */
@Path("/v1/savings/{savingsId}/momo")
@Component
@Scope("singleton")
@Slf4j
@RequiredArgsConstructor
public class SavingsMomoApiResource {

    private final PlatformSecurityContext context;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<Map<String, Object>> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final YoPaymentSavingsDepositIntegrationService yoPaymentSavingsDepositIntegrationService;

    /**
     * Initiates a mobile money deposit to a savings account via Yo Payments
     */
    @POST
    @Path("deposit")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String initiateDeposit(@PathParam("savingsId") final Long savingsId, final String apiRequestBodyAsJson,
            @Context final UriInfo uriInfo) {
        try {
            // Validate user has permission
            this.context.authenticatedUser().validateHasReadPermission(SavingsApiConstants.SAVINGS_ACCOUNT_RESOURCE_NAME);

            // Parse deposit parameters from JSON
            JsonElement jsonElement = fromApiJsonHelper.parse(apiRequestBodyAsJson);

            // Validate required fields
            validateMobileMoneyDepositRequest(jsonElement);

            // Get amount, mobile number and note from request
            final BigDecimal amount = fromApiJsonHelper.extractBigDecimalWithLocaleNamed("transactionAmount", jsonElement);
            final String mobileNumber = fromApiJsonHelper.extractStringNamed("mobileNumber", jsonElement);
            final String note = fromApiJsonHelper.extractStringNamed("note", jsonElement);

            // Get transaction date
            LocalDate transactionDate;
            if (fromApiJsonHelper.parameterExists("transactionDate", jsonElement)) {
                transactionDate = fromApiJsonHelper.extractLocalDateNamed("transactionDate", jsonElement);
            } else {
                transactionDate = DateUtils.getBusinessLocalDate();
            }

            // Get the savings account
            final SavingsAccount savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsId, false);
            if (savingsAccount == null) {
                throw new SavingsAccountNotFoundException(savingsId);
            }

            // Prepare the payment data
            MomoPaymentData momoPaymentData = new MomoPaymentData();
            momoPaymentData.setAmount(amount);
            momoPaymentData.setMobileNumber(mobileNumber);
            momoPaymentData.setNote(note);

            // Process the deposit through Yo Payment
            SavingsAccountTransaction transaction = yoPaymentSavingsDepositIntegrationService.processDeposit(momoPaymentData,
                    savingsAccount);

            // Return result
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("savingsId", savingsId);
            responseMap.put("resourceId", transaction != null ? transaction.getId() : null);
            responseMap.put("status", transaction != null ? "SUCCESS" : "PENDING");

            final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
            return this.toApiJsonSerializer.serialize(settings, responseMap);

        } catch (IOException e) {
            log.error("Error processing mobile money deposit: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing mobile money deposit", e);
        }
    }

    /**
     * Validates the deposit request parameters
     */
    private void validateMobileMoneyDepositRequest(JsonElement element) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource("savings.momo.deposit");

        if (!fromApiJsonHelper.parameterExists("transactionAmount", element)) {
            baseDataValidator.reset().parameter("transactionAmount").value(null).notNull();
        } else {
            BigDecimal amount = fromApiJsonHelper.extractBigDecimalWithLocaleNamed("transactionAmount", element);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                baseDataValidator.reset().parameter("transactionAmount").value(amount).positiveAmount();
            }
        }

        if (!fromApiJsonHelper.parameterExists("mobileNumber", element)) {
            baseDataValidator.reset().parameter("mobileNumber").value(null).notBlank();
        } else {
            String mobileNumber = fromApiJsonHelper.extractStringNamed("mobileNumber", element);
            if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
                baseDataValidator.reset().parameter("mobileNumber").value(mobileNumber).notBlank();
            }
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist", dataValidationErrors);
        }
    }
}
 
