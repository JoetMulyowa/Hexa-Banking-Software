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
package org.apache.fineract.infrastructure.momo.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class ProcessLoanTransactionsOnMomoPaymentTasklet implements Tasklet {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessLoanTransactionsOnMomoPaymentTasklet.class);

    private final ConfigurationDomainService configurationDomainService;
    private final LoanReadPlatformService loanReadPlatformService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LOG.info("Started processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());

        // TODO: Implement the business logic for processing loan transactions on Momo payment

        LOG.info("Completed processing loan transactions on Momo payment at {}", DateUtils.getLocalDateTimeOfTenant());
        return RepeatStatus.FINISHED;
    }
}
