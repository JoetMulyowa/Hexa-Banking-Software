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
package org.apache.fineract.infrastructure.momo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

/**
 * Entity for tracking savings account deposits via Yo Payments
 */
@Entity
@Table(name = "m_yo_payment_savings_deposit_transaction")
@Getter
@Setter
@NoArgsConstructor
public class YoPaymentSavingsDepositTransaction extends AbstractAuditableCustom {

    @ManyToOne
    @JoinColumn(name = "savings_account_id", nullable = false)
    private SavingsAccount savingsAccount;

    @ManyToOne
    @JoinColumn(name = "savings_transaction_id", nullable = true)
    private SavingsAccountTransaction savingsTransaction;

    @Column(name = "vendor_tran_id", length = 100, nullable = true)
    private String vendorTransactionId;

    @Column(name = "middleware_reference_no", length = 100, nullable = true)
    private String middlewareReferenceNo;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;

    @Column(name = "status_code", length = 10, nullable = true)
    private String statusCode;

    @Column(name = "status_desc", length = 100, nullable = true)
    private String statusDescription;

    @Column(name = "mobile_number", length = 20, nullable = false)
    private String mobileNumber;

    @Column(name = "request_body", nullable = true)
    private String requestBody;

    @Column(name = "response_body", nullable = true)
    private String responseBody;

    @Column(name = "error_msg", length = 500, nullable = true)
    private String errorMessage;

    /**
     * Creates a new YoPaymentSavingsDepositTransaction for tracking a deposit
     */
    public static YoPaymentSavingsDepositTransaction create(SavingsAccount savingsAccount, BigDecimal amount, String mobileNumber,
            LocalDateTime transactionDate) {
        YoPaymentSavingsDepositTransaction transaction = new YoPaymentSavingsDepositTransaction();
        transaction.savingsAccount = savingsAccount;
        transaction.amount = amount;
        transaction.mobileNumber = mobileNumber;
        transaction.transactionDate = transactionDate;
        transaction.reversed = false;
        return transaction;
    }

    /**
     * Update transaction with request details
     */
    public void updateRequest(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Update transaction with response details
     */
    public void updateResponse(String responseBody, String vendorTransactionId, String statusCode, String statusDescription) {
        this.responseBody = responseBody;
        this.vendorTransactionId = vendorTransactionId;
        this.statusCode = statusCode;
        this.statusDescription = statusDescription;
    }

    /**
     * Update transaction with error details
     */
    public void updateError(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Link with a Fineract savings transaction
     */
    public void linkSavingsTransaction(SavingsAccountTransaction savingsTransaction) {
        this.savingsTransaction = savingsTransaction;
    }

    /**
     * Mark the transaction as reversed
     */
    public void markAsReversed() {
        this.reversed = true;
    }
}
