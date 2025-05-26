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
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SurePayMomoPaymentIntegrationWritePlatformServiceImpl extends MomoPaymentIntegrationWritePlatformServiceImpl
        implements SurePayMomoPaymentIntegrationWritePlatformService {

    private final MomoService momoService;

    @Autowired
    public SurePayMomoPaymentIntegrationWritePlatformServiceImpl(GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper,
            LoanRepositoryWrapper loanRepositoryWrapper, MomoLoanPaymentTransactionRepository loanPaymentTransactionRepository,
            SurePayMomoService momoService) {
        super(configurationRepositoryWrapper, loanRepositoryWrapper, loanPaymentTransactionRepository);
        this.momoService = momoService;
    }

    @Override
    protected void processPayment(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoLoanPaymentTransaction)
            throws IOException {
        this.momoService.handle(momoPaymentData, momoLoanPaymentTransaction);
    }

    @Override
    protected boolean isEnabled() {
        return configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SURE_MOBILE_MONEY_PAYMENT).isEnabled();
    }

    @Override
    public String getPaymentTypeCode() {
        return "SURE_PAY_MOMO_PAYMENT";
    }
}
