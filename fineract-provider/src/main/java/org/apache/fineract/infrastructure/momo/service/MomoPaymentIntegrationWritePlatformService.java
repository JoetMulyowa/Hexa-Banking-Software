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
 * Interface for Mobile Money Payment Integration
 */
public interface MomoPaymentIntegrationWritePlatformService {

    /**
     * Process a payout through Mobile Money API
     *
     * @param momoPaymentData
     *            the payment data object
     * @param loan
     *            the loan object
     * @param loanTransaction
     *            the loan transaction
     * @throws IOException
     *             if there's an issue with API communication
     */
    void payOut(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction) throws IOException;

    /**
     * Get the payment type code that this implementation handles
     *
     * @return the payment type code (e.g., "SURE_PAY_MOMO_PAYMENT", "YO_PAYMENT_MOMO_PAYMENT")
     */
    String getPaymentTypeCode();
}
