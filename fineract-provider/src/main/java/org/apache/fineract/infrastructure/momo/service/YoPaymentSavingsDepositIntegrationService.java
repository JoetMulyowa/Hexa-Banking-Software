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
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

/**
 * Interface for Yo Payment Mobile Money Savings Deposit Integration
 */
public interface YoPaymentSavingsDepositIntegrationService {

    /**
     * Process a deposit through Yo Payment's Momo API
     *
     * @param momoPaymentData
     *            the payment data
     * @param savingsAccount
     *            the savings account
     * @return the created savings transaction if successful, or null if pending
     * @throws IOException
     *             if there's an issue with API communication
     */
    SavingsAccountTransaction processDeposit(MomoPaymentData momoPaymentData, SavingsAccount savingsAccount) throws IOException;

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
