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

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.springframework.stereotype.Service;

/**
 * Factory service that manages different mobile money payment providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentProviderFactory {

    private final List<MomoPaymentIntegrationWritePlatformService> momoPaymentProviders;
    private final List<YoPaymentRepaymentIntegrationService> yoPaymentRepaymentProviders;
    private final Map<String, MomoPaymentIntegrationWritePlatformService> disbursementProviderMap = new HashMap<>();
    private final Map<String, YoPaymentRepaymentIntegrationService> repaymentProviderMap = new HashMap<>();

    /**
     * Initialize the provider map after dependency injection
     */
    @PostConstruct
    public void init() {
        // Register disbursement providers
        for (MomoPaymentIntegrationWritePlatformService provider : momoPaymentProviders) {
            disbursementProviderMap.put(provider.getPaymentTypeCode(), provider);
            log.info("Registered mobile money disbursement provider: {}", provider.getPaymentTypeCode());
        }

        // Register repayment providers
        for (YoPaymentRepaymentIntegrationService provider : yoPaymentRepaymentProviders) {
            repaymentProviderMap.put(provider.getPaymentTypeCode(), provider);
            log.info("Registered mobile money repayment provider: {}", provider.getPaymentTypeCode());
        }
    }

    /**
     * Get the appropriate payment provider for disbursements by payment type code
     *
     * @param paymentTypeCode
     *            the code identifying the payment provider
     * @return the payment provider service
     * @throws GeneralPlatformDomainRuleException
     *             if no provider is found for the given code
     */
    public MomoPaymentIntegrationWritePlatformService getProvider(String paymentTypeCode) {
        MomoPaymentIntegrationWritePlatformService provider = disbursementProviderMap.get(paymentTypeCode);

        if (provider == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.momo.provider.not.found",
                    "Mobile money payment provider not found for type: " + paymentTypeCode);
        }

        return provider;
    }

    /**
     * Get the appropriate payment provider for repayments by payment type code
     *
     * @param paymentTypeCode
     *            the code identifying the payment provider
     * @return the payment provider service
     * @throws GeneralPlatformDomainRuleException
     *             if no provider is found for the given code
     */
    public YoPaymentRepaymentIntegrationService getRepaymentProvider(String paymentTypeCode) {
        YoPaymentRepaymentIntegrationService provider = repaymentProviderMap.get(paymentTypeCode);

        if (provider == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.momo.repayment.provider.not.found",
                    "Mobile money repayment provider not found for type: " + paymentTypeCode);
        }

        return provider;
    }
}
