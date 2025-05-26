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

/**
 * Data object for Yo Payment API responses
 */
@Data
@NoArgsConstructor
public class YoPaymentResponse {

    // Standard fields for all response types
    private String status;
    private Integer statusCode;
    private String statusMessage;
    private Integer errorMessageCode;
    private String errorMessage;
    private String transactionStatus;
    private String transactionReference;

    // Fields specifically for successful withdrawals
    private String mNOTransactionReferenceId;

    // Fields that might be present in error responses
    private String accountNumber;
    private String amountWithdrawn;
    private String balance;
    private String currency;
    private String narrativeText;
}
