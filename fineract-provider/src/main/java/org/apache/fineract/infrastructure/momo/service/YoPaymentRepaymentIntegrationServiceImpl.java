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
import org.apache.fineract.infrastructure.configuration.api.GlobalConfigurationConstants;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.momo.data.MomoPaymentData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the YoPaymentRepaymentIntegrationService for loan repayments via Yo Payment
 */
@Service
@Slf4j
public class YoPaymentRepaymentIntegrationServiceImpl implements YoPaymentRepaymentIntegrationService {

    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    private final YoPaymentRepaymentService yoPaymentRepaymentService;

    @Autowired
    public YoPaymentRepaymentIntegrationServiceImpl(GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            YoPaymentRepaymentService yoPaymentRepaymentService) {
        this.configurationRepositoryWrapper = configurationRepositoryWrapper;
        this.yoPaymentRepaymentService = yoPaymentRepaymentService;
    }

    @Override
    public void processRepayment(MomoPaymentData momoPaymentData, Loan loan, LoanTransaction loanTransaction) throws IOException {
        if (!isEnabled()) {
            log.info("Yo Payment Mobile Money repayment is disabled");
            return;
        }

        // Process the repayment via Yo Payment
        log.info("Processing loan repayment via Yo Payment for loan: {}, transaction: {}", loan.getId(), loanTransaction.getId());

        yoPaymentRepaymentService.processRepayment(momoPaymentData, loan, loanTransaction);
    }

    @Override
    public boolean isEnabled() {
        return configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_YO_PAYMENT_MOBILE_MONEY_PAYMENT).isEnabled();
    }

    @Override
    public String getPaymentTypeCode() {
        return "YO_PAYMENT_MOMO_PAYMENT";
    }
}
