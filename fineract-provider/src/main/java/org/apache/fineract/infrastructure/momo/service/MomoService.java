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
import org.apache.fineract.infrastructure.momo.domain.MomoLoanPaymentTransaction;

/**
 * Service for handling mobile money payment operations
 */
public interface MomoService {

    /**
     * Process a mobile money payment request
     *
     * @param momoPaymentData
     *            The payment data
     * @param momoLoanPaymentTransaction
     *            The transaction record
     * @throws IOException
     *             If there's an error communicating with the payment provider
     */
    void handle(MomoPaymentData momoPaymentData, MomoLoanPaymentTransaction momoLoanPaymentTransaction) throws IOException;
}
