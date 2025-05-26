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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.apache.fineract.infrastructure.momo.domain.MomoStatusEnum;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;

/**
 * Base implementation of mobile money payment service
 */
@Slf4j
@RequiredArgsConstructor
public abstract class MomoPaymentIntegrationWritePlatformServiceImpl implements MomoPaymentIntegrationWritePlatformService {

    protected final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    protected final LoanRepositoryWrapper loanRepositoryWrapper;
    protected final MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository;

    @Override
    public void payOut(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction transaction) throws IOException {
        log.info("Processing momo payment for loan: {}, transaction: {}", loan.getId(), transaction.getId());

        if (!isEnabled()) {
            log.info("Mobile money payment integration is disabled");
            return;
        }

        try {
            // Create a record of the payment transaction
            MomoLoanPaymentTransaction momoLoanPaymentTransaction = MomoLoanPaymentTransaction.instance(transaction, loan,
                    momoPaymentData.getAccountNumber(), momoPaymentData.getTranAmount(), MomoStatusEnum.PENDING.getValue(),
                    momoPaymentData.getTranNarration(), null, null, "PENDING");

            // Save initial transaction record
            loanPaymentTransactionRepository.saveAndFlush(momoLoanPaymentTransaction);

            // Process the payment via the specific provider
            processPayment(momoPaymentData, momoLoanPaymentTransaction);

        } catch (Exception e) {
            log.error("Error processing mobile money payment: {}", e.getMessage(), e);
            throw new IOException("Error processing mobile money payment: " + e.getMessage(), e);
        }
    }

    /**
     * Process the payment using the specific provider implementation
     *
     * @param momoPaymentData
     *            the payment data
     * @param momoLoanPaymentTransaction
     *            the transaction record
     * @throws IOException
     *             if an error occurs during payment processing
     */
    protected abstract void processPayment(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoLoanPaymentTransaction)
            throws IOException;

    /**
     * Check if this payment provider is enabled
     *
     * @return true if enabled, false otherwise
     */
    protected abstract boolean isEnabled();
}
