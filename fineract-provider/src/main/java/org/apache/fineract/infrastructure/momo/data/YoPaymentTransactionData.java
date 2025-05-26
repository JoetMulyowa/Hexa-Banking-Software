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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentRepaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.YoPaymentTransaction;

/**
 * Data Transfer Object for Yo Payment transaction data. Used for API serialization.
 */
@Data
@AllArgsConstructor
public class YoPaymentTransactionData {

    private Long id;
    private Long loanId;
    private Long loanTransactionId;
    private String middlewareReferenceNo;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private Boolean reversed;
    private String statusCode;
    private String statusDesc;
    private String vendorTranId;
    private String errorMsg;

    /**
     * Convert a YoPaymentTransaction domain entity to a YoPaymentTransactionData DTO
     *
     * @param transaction
     *            the YoPaymentTransaction entity
     * @return a new YoPaymentTransactionData instance
     */
    public static YoPaymentTransactionData from(YoPaymentTransaction transaction) {
        return new YoPaymentTransactionData(transaction.getId(), transaction.getLoan().getId(),
                transaction.getLoanTransaction() != null ? transaction.getLoanTransaction().getId() : null,
                transaction.getMiddlewareReferenceNo(), transaction.getDateOf(), transaction.getAmount(), transaction.getReversed(),
                transaction.getStatusCode(), transaction.getStatusDesc(), transaction.getVendorTranId(), transaction.getErrorMsg());
    }

    /**
     * Convert a YoPaymentRepaymentTransaction domain entity to a YoPaymentTransactionData DTO
     *
     * @param transaction
     *            the YoPaymentRepaymentTransaction entity
     * @return a new YoPaymentTransactionData instance
     */
    public static YoPaymentTransactionData from(YoPaymentRepaymentTransaction transaction) {
        return new YoPaymentTransactionData(transaction.getId(), transaction.getLoan().getId(),
                transaction.getLoanTransaction() != null ? transaction.getLoanTransaction().getId() : null,
                transaction.getMiddlewareReferenceNo(), transaction.getDateOf(), transaction.getAmount(), transaction.getReversed(),
                transaction.getStatusCode(), transaction.getStatusDesc(), transaction.getVendorTranId(), transaction.getErrorMsg());
    }
}
