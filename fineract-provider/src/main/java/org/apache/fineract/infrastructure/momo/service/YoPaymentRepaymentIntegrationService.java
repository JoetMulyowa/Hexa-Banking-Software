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
package org.apache.fineract.infrastructure.momo.service;

import java.io.IOException;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;

/**
 * Interface for Yo Payment Mobile Money Repayment Integration
 */
public interface YoPaymentRepaymentIntegrationService {

    /**
     * Process a repayment (deposit) through Yo Payment's Momo API
     *
     * @param momoPaymentData
     *            the payment data
     * @param loan
     *            the loan
     * @param loanTransaction
     *            the loan transaction
     * @throws IOException
     *             if there's an issue with API communication
     */
    void processRepayment(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction) throws IOException;

    /**
     * Check if this integration is enabled
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();

    /**
     * Get the payment type code
     *
     * @return the payment type code
     */
    String getPaymentTypeCode();
}
