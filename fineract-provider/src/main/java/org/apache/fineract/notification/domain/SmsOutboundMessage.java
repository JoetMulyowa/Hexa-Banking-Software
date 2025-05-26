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
package org.apache.fineract.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_sms_outbound_message")
@Getter
@Setter
@NoArgsConstructor
public class SmsOutboundMessage extends AbstractPersistableCustom {

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "status_enum", nullable = false)
    private Integer statusEnum;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "campaign_name")
    private String campaignName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submitted_on_date", nullable = false)
    private Date submittedOnDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "delivered_on_date")
    private Date deliveredOnDate;

    @Column(name = "provider")
    private String provider;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "sms_type")
    private String smsType;

    @Column(name = "message_cost")
    private String messageCost;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "message_parts")
    private Integer messageParts;

    @Column(name = "reference_entity_type")
    private String referenceEntityType;

    @Column(name = "reference_entity_id")
    private Long referenceEntityId;

    // Constructor for creating a new SMS message record
    public SmsOutboundMessage(String recipient, String message, String smsType, String referenceEntityType, Long referenceEntityId) {
        this.recipient = recipient;
        this.message = message;
        this.statusEnum = 100; // Pending
        this.submittedOnDate = new Date();
        this.provider = "AFRICASTALKING";
        this.smsType = smsType;
        this.referenceEntityType = referenceEntityType;
        this.referenceEntityId = referenceEntityId;
    }

    // Update with delivery information
    public void updateDeliveryStatus(String messageId, String statusCode, String messageCost, Integer messageParts) {
        this.messageId = messageId;
        this.statusCode = statusCode;
        this.messageCost = messageCost;
        this.messageParts = messageParts;
        this.statusEnum = "Success".equalsIgnoreCase(statusCode) ? 200 : 300; // 200=Delivered, 300=Failed
        this.deliveredOnDate = new Date();
    }

    // Update with error information
    public void updateWithError(String errorMessage) {
        this.errorMessage = errorMessage;
        this.statusEnum = 300; // Failed
        this.deliveredOnDate = new Date();
    }
}
