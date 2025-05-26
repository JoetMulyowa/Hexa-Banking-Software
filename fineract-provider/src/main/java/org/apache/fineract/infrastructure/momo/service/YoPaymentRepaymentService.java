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
import org.apache.fineract.infrastructure.momo.domain.YoPaymentRepaymentTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;

/**
 * Service interface for handling Yo Payment repayments
 */
public interface YoPaymentRepaymentService {

    /**
     * Process a loan repayment through Yo Payment's mobile money API
     *
     * @param momoPaymentData
     *            the payment data
     * @param loan
     *            the loan
     * @param loanTransaction
     *            the loan transaction
     * @return the created Yo Payment transaction record
     * @throws IOException
     *             if there's an error communicating with the API
     */
    YoPaymentRepaymentTransaction processRepayment(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction)
            throws IOException;

    /**
     * Check the status of a pending repayment transaction
     *
     * @param transactionReference
     *            the transaction reference to check
     * @return the updated transaction record
     * @throws IOException
     *             if there's an error communicating with the API
     */
    YoPaymentRepaymentTransaction checkRepaymentStatus(String transactionReference) throws IOException;
}
