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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.momo.data.YoPaymentTransactionData;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentRepaymentTransaction;
import org.apache.fineract.infrastructure.momo.service.YoPaymentRepaymentService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.stereotype.Component;

/**
 * REST API for mobile money operations
 */
@Path("/v1/momo")
@Component
@RequiredArgsConstructor
@Slf4j
public class MomoApiResource {

    private final PlatformSecurityContext context;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final DefaultToApiJsonSerializer<YoPaymentTransactionData> toApiJsonSerializer;
    private final YoPaymentRepaymentService yoPaymentRepaymentService;

    /**
     * Check the status of a Yo Payment transaction
     *
     * Example Requests:
     *
     * /momo/yo/status/ab12cd34ef56
     *
     * @param transactionReference
     *            the transaction reference ID
     * @return details of the transaction including current status
     */
    @GET
    @Path("yo/status/{transactionReference}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String checkYoPaymentTransactionStatus(@Context final UriInfo uriInfo,
            @PathParam("transactionReference") final String transactionReference) {
        this.context.authenticatedUser();

        try {
            YoPaymentRepaymentTransaction transaction = this.yoPaymentRepaymentService.checkRepaymentStatus(transactionReference);

            // Convert to data object for API serialization
            YoPaymentTransactionData transactionData = YoPaymentTransactionData.from(transaction);

            final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
            return this.toApiJsonSerializer.serialize(settings, transactionData);
        } catch (IOException e) {
            log.error("Error checking Yo Payment transaction status: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking transaction status: " + e.getMessage(), e);
        }
    }
}
