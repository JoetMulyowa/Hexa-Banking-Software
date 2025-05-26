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

import java.util.List;
import java.util.Optional;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing YoPaymentTransaction entities
 */
@Repository
public interface YoPaymentTransactionRepository
        extends JpaRepository<YoPaymentTransaction, Long>, JpaSpecificationExecutor<YoPaymentTransaction> {

    /**
     * Find transaction by middleware reference number
     *
     * @param middlewareReferenceNo
     *            The middleware reference number
     * @return The matching transaction if found
     */
    Optional<YoPaymentTransaction> findByMiddlewareReferenceNo(String middlewareReferenceNo);

    /**
     * Find transaction by vendor transaction ID
     *
     * @param vendorTranId
     *            The vendor transaction ID
     * @return The matching transaction if found
     */
    Optional<YoPaymentTransaction> findByVendorTranId(String vendorTranId);

    /**
     * Find all transactions for a specific loan
     *
     * @param loan
     *            The loan
     * @return List of transactions for the loan
     */
    List<YoPaymentTransaction> findByLoan(Loan loan);

    /**
     * Find the transaction associated with a specific loan transaction
     *
     * @param loanTransaction
     *            The loan transaction
     * @return The matching transaction if found
     */
    Optional<YoPaymentTransaction> findByLoanTransaction(LoanTransaction loanTransaction);
}
