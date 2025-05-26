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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;

/**
 * Entity class for storing Yo Payment transaction data
 */
@Entity
@Table(name = "m_yo_payment_transaction")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YoPaymentTransaction extends AbstractAuditableCustom {

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    Loan loan;

    @ManyToOne
    @JoinColumn(name = "loan_transaction_id", nullable = true)
    LoanTransaction loanTransaction;

    @Column(name = "middleware_reference_no", length = 100)
    String middlewareReferenceNo;

    @Column(name = "transaction_date", nullable = false)
    LocalDate dateOf;

    @Column(name = "amount", nullable = false, scale = 6, precision = 19)
    BigDecimal amount;

    @Column(name = "is_reversed", nullable = false)
    Boolean reversed;

    @Column(name = "status_code", length = 10)
    String statusCode;

    @Column(name = "status_desc", length = 100)
    String statusDesc;

    @Column(name = "request_body", columnDefinition = "TEXT")
    String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    String responseBody;

    @Column(name = "error_msg", length = 500)
    String errorMsg;

    @Column(name = "vendor_tran_id", length = 100)
    String vendorTranId;
}
