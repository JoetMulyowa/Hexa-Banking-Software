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

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MomoPaymentData {

    private String accountNumber;
    private BigDecimal tranAmount;
    private String accountType;
    private String tranType;
    private String currency;
    private String accountName;
    private String paymentDate;
    private String password;
    private String tranSignature;
    private String vendorCode;
    private String telecom;
    private String vendorTranId;
    private String tranNarration;

    public MomoPaymentData(String accountNumber, BigDecimal tranAmount, String accountType, String tranType, String currency,
            String accountName, String paymentDate, String vendorTranId, String tranNarration) {
        this.accountNumber = accountNumber;
        this.tranAmount = tranAmount;
        this.accountType = accountType;
        this.tranType = tranType;
        this.currency = currency;
        this.accountName = accountName;
        this.paymentDate = paymentDate;
        this.vendorTranId = vendorTranId;
        this.tranNarration = tranNarration;
    }
}
