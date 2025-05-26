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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface YoPaymentSavingsDepositTransactionRepository
        extends JpaRepository<YoPaymentSavingsDepositTransaction, Long>, JpaSpecificationExecutor<YoPaymentSavingsDepositTransaction> {

    /**
     * Find a transaction by its vendor transaction ID
     */
    Optional<YoPaymentSavingsDepositTransaction> findByVendorTransactionId(String vendorTransactionId);

    /**
     * Find a transaction by its middleware reference number
     */
    Optional<YoPaymentSavingsDepositTransaction> findByMiddlewareReferenceNo(String middlewareReferenceNo);

    /**
     * Find transactions for a savings account
     */
    List<YoPaymentSavingsDepositTransaction> findBySavingsAccountIdOrderByTransactionDateDesc(Long savingsAccountId);

    /**
     * Find transactions that need processing (status check)
     */
    @Query("SELECT t FROM YoPaymentSavingsDepositTransaction t WHERE t.savingsTransaction IS NULL AND t.reversed = false AND t.statusCode = :statusCode")
    List<YoPaymentSavingsDepositTransaction> findPendingTransactions(@Param("statusCode") String statusCode);
}
