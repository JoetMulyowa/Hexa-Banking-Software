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
package org.apache.fineract.infrastructure.momo.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MomoPaymentResponse {

    private String vendorTranId;
    private String tranReference;
    private String statusCode;
    private String statusDesc;
    private String processorId;
    private String tranCharge;
    private String msisdn;
    private String customerName;
    private String tranCode;
    private String vendorCode;
    private String gatewayRef;
    private String fromAccount;
    private String toAccount;
    private String tranType;
    private String currency;
    private String accountType;
    private String recordDate;
    private String network;
    private String tranNarration;
    private String tranStatus;
    private String reason;
    private String telecomResponseDate;
    private String suspiciousStatus;
    private String returnUrl;
    private String ovaAffected;
    private String tranAmount;
    private String convertedAmount;

}
