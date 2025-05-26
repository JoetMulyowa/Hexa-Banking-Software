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
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the YoPaymentSavingsDepositIntegrationService for savings account deposits via Yo Payment
 */
@Service
@Slf4j
public class YoPaymentSavingsDepositIntegrationServiceImpl implements YoPaymentSavingsDepositIntegrationService {

    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final YoPaymentSavingsDepositService yoPaymentSavingsDepositService;

    @Autowired
    public YoPaymentSavingsDepositIntegrationServiceImpl(GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            YoPaymentSavingsDepositService yoPaymentSavingsDepositService) {
        this.configurationRepositoryWrapper = configurationRepositoryWrapper;
        this.yoPaymentSavingsDepositService = yoPaymentSavingsDepositService;
    }

    @Override
    public SavingsAccountTransaction processDeposit(MomoPaymentData momoPaymentData, SavingsAccount savingsAccount) throws IOException {
        if (!isEnabled()) {
            log.info("Yo Payment Mobile Money savings deposit is disabled");
            return null;
        }

        // Process the deposit via Yo Payment
        log.info("Processing savings deposit via Yo Payment for account: {}", savingsAccount.getId());

        // Call the Yo Payment service to process the deposit
        // The actual Fineract savings account transaction will be created after successful payment
        // or immediately if the payment is successful right away
        var depositTransaction = yoPaymentSavingsDepositService.processDeposit(momoPaymentData, savingsAccount);

        // Return the linked savings transaction if available (may be null if payment is pending)
        return depositTransaction.getSavingsTransaction();
    }

    @Override
    public boolean isEnabled() {
        return configurationRepositoryWrapper.findOneByNameWithNotFoundDetection("enable-yo-payment-savings-deposit").isEnabled();
    }

    @Override
    public String getPaymentTypeCode() {
        return "YO_PAYMENT_MOMO_PAYMENT";
    }
}
