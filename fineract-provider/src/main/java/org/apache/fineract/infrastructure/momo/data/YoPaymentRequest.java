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

/**
 * Data object for Yo Payment API requests This class represents the XML structure to be sent to Yo Payments API
 */
@Data
@NoArgsConstructor
public class YoPaymentRequest {

    // Required parameters for all API requests
    private String apiUsername;
    private String apiPassword;
    private String method;

    // Parameters for withdraw funds transaction
    private String nonBlocking;
    private BigDecimal amount;
    private String account;
    private String accountProviderCode;
    private String providerCode;
    private String narrative;
    private String externalReference;
    private String transactionReference;

    /**
     * Create a new YoPaymentRequest for withdrawing funds
     *
     * @param apiUsername
     *            API username
     * @param apiPassword
     *            API password
     * @param account
     *            The account (mobile money number) to withdraw to
     * @param amount
     *            The amount to withdraw
     * @param narrative
     *            Description of the transaction
     * @param externalReference
     *            External reference for the transaction
     */
    public YoPaymentRequest(String apiUsername, String apiPassword, String account, BigDecimal amount, String narrative,
            String externalReference) {
        this.apiUsername = apiUsername;
        this.apiPassword = apiPassword;
        this.method = "acwithdrawfunds";
        this.nonBlocking = "FALSE";
        this.account = account;
        this.amount = amount;
        this.narrative = narrative;
        this.externalReference = externalReference;
    }
}
