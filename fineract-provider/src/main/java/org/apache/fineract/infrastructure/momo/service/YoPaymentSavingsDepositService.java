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
import org.apache.fineract.infrastructure.momo.domain.YoPaymentSavingsDepositTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

/**
 * Service interface for processing savings deposits via Yo Payment
 */
public interface YoPaymentSavingsDepositService {

    /**
     * Process a deposit request via Yo Payment
     *
     * @param momoPaymentData
     *            The payment data containing amount, mobile number, etc.
     * @param savingsAccount
     *            The savings account to deposit into
     * @return A transaction record for tracking
     * @throws IOException
     *             If there's an issue with API communication
     */
    YoPaymentSavingsDepositTransaction processDeposit(MomoPaymentData momoPaymentData, SavingsAccount savingsAccount) throws IOException;

    /**
     * Check the status of a pending deposit transaction
     *
     * @param transactionReference
     *            The transaction reference to check
     * @return true if the transaction was successful and processed
     * @throws IOException
     *             If there's an issue with API communication
     */
    boolean checkDepositStatus(String transactionReference) throws IOException;

    /**
     * Complete a deposit after successful payment
     *
     * @param depositTransaction
     *            The deposit transaction record
     * @return The savings account transaction created in Fineract
     */
    SavingsAccountTransaction completeDeposit(YoPaymentSavingsDepositTransaction depositTransaction);
}
