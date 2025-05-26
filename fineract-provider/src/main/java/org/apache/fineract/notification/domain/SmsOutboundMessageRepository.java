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

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsOutboundMessageRepository
        extends JpaRepository<SmsOutboundMessage, Long>, JpaSpecificationExecutor<SmsOutboundMessage> {

    List<SmsOutboundMessage> findByRecipient(String recipient);

    List<SmsOutboundMessage> findByReferenceEntityTypeAndReferenceEntityId(String referenceEntityType, Long referenceEntityId);

    List<SmsOutboundMessage> findByStatusEnum(Integer statusEnum);

    @Query("SELECT s FROM SmsOutboundMessage s WHERE s.submittedOnDate BETWEEN :startDate AND :endDate")
    List<SmsOutboundMessage> findMessagesBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT s FROM SmsOutboundMessage s WHERE s.referenceEntityType = :entityType AND s.referenceEntityId = :entityId AND s.smsType = :smsType ORDER BY s.submittedOnDate DESC")
    List<SmsOutboundMessage> findLatestMessagesByEntityAndType(@Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("smsType") String smsType);
}
