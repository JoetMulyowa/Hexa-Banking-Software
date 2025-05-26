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
package org.apache.fineract.notification.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.domain.SmsOutboundMessage;
import org.apache.fineract.notification.domain.SmsOutboundMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Path("/v1/sms")
@Component
@Scope("singleton")
public class SmsHistoryApiResource {

    private final SmsOutboundMessageRepository smsOutboundMessageRepository;
    private final DefaultToApiJsonSerializer<Object> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PlatformSecurityContext context;

    @Autowired
    public SmsHistoryApiResource(SmsOutboundMessageRepository smsOutboundMessageRepository,
            DefaultToApiJsonSerializer<Object> toApiJsonSerializer, ApiRequestParameterHelper apiRequestParameterHelper,
            PlatformSecurityContext context) {
        this.smsOutboundMessageRepository = smsOutboundMessageRepository;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.context = context;
    }

    @GET
    @Path("history")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveSmsHistory(@Context final UriInfo uriInfo, @QueryParam("limit") @DefaultValue("100") final int limit,
            @QueryParam("offset") @DefaultValue("0") final int offset) {

        this.context.authenticatedUser().validateHasReadPermission("SMS_HISTORY");

        Pageable pageable = PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "submittedOnDate"));
        Page<SmsOutboundMessage> messages = this.smsOutboundMessageRepository.findAll(pageable);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        Map<String, Object> response = new HashMap<>();
        response.put("totalFilteredRecords", messages.getTotalElements());
        response.put("pageItems", messages.getContent());

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    @GET
    @Path("history/entity/{entityType}/{entityId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveSmsHistoryByEntity(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission("SMS_HISTORY");

        List<SmsOutboundMessage> messages = this.smsOutboundMessageRepository.findByReferenceEntityTypeAndReferenceEntityId(entityType,
                entityId);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        Map<String, Object> response = new HashMap<>();
        response.put("entityType", entityType);
        response.put("entityId", entityId);
        response.put("totalFilteredRecords", messages.size());
        response.put("pageItems", messages);

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    @GET
    @Path("history/phone/{phoneNumber}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveSmsHistoryByPhone(@PathParam("phoneNumber") final String phoneNumber, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission("SMS_HISTORY");

        List<SmsOutboundMessage> messages = this.smsOutboundMessageRepository.findByRecipient(phoneNumber);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        Map<String, Object> response = new HashMap<>();
        response.put("phoneNumber", phoneNumber);
        response.put("totalFilteredRecords", messages.size());
        response.put("pageItems", messages);

        return this.toApiJsonSerializer.serialize(settings, response);
    }

    @GET
    @Path("history/date")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveSmsHistoryByDateRange(@QueryParam("startDate") final String startDateStr,
            @QueryParam("endDate") final String endDateStr, @Context final UriInfo uriInfo) {

        this.context.authenticatedUser().validateHasReadPermission("SMS_HISTORY");

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        Date startDateTime = java.sql.Date.valueOf(startDate);
        Date endDateTime = java.sql.Date.valueOf(endDate.plusDays(1));

        List<SmsOutboundMessage> messages = this.smsOutboundMessageRepository.findMessagesBetweenDates(startDateTime, endDateTime);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDateStr);
        response.put("endDate", endDateStr);
        response.put("totalFilteredRecords", messages.size());
        response.put("pageItems", messages);

        return this.toApiJsonSerializer.serialize(settings, response);
    }
}
