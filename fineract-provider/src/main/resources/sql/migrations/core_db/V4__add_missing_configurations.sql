--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements. See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership. The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License. You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied. See the License for the
-- specific language governing permissions and limitations
-- under the License.
--

-- Insert missing configuration properties
INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'change-emi-if-repayment-date-same-as-disbursement-date', NULL, NULL, 0, 0, 'Change EMI if repayment date is same as disbursement date'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'change-emi-if-repayment-date-same-as-disbursement-date'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'loan-reschedule-is-first-payday-allowed-on-holiday', NULL, NULL, 0, 0, 'Is the first repayment day of the rescheduled loan allowed to fall on a holiday or a non-working day?'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'loan-reschedule-is-first-payday-allowed-on-holiday'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'interest-charged-from-date-same-as-disbursal-date', NULL, NULL, 0, 0, 'Interest charged from date same as disbursal date'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'interest-charged-from-date-same-as-disbursal-date'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'payment-type-applicable-for-disbursement-charges', NULL, NULL, 0, 0, 'Is payment type applicable for disbursement charges?'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'payment-type-applicable-for-disbursement-charges'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'daily-tpt-limit', NULL, NULL, 0, 0, 'Daily third party transfer limit'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'daily-tpt-limit'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'allow-backdated-transaction-before-interest-posting-date-for-days', NULL, NULL, 0, 0, 'Number of days allowed for backdated transactions before interest posting date'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'allow-backdated-transaction-before-interest-posting-date-for-days'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'sub-rates', NULL, NULL, 0, 0, 'Enable sub-rates for loan products'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'sub-rates'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'account-mapping-for-payment-type', NULL, NULL, 0, 0, 'Account mapping for payment type'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'account-mapping-for-payment-type'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'account-mapping-for-charge', NULL, NULL, 0, 0, 'Account mapping for charge'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'account-mapping-for-charge'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'fixed-deposit-transfer-interest-next-day-for-period-end-posting', NULL, NULL, 0, 0, 'Should fixed deposit transfer interest to next day for period end posting?'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'fixed-deposit-transfer-interest-next-day-for-period-end-posting'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'custom-account-number-length', NULL, NULL, 0, 0, 'Custom account number length'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'custom-account-number-length'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'random-account-number', NULL, NULL, 0, 0, 'Generate random account numbers?'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'random-account-number'
);

-- Add mobile money payment configurations
INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'enable-sure-mobile-money-payment', NULL, NULL, 1, 0, 'Enable SurePay Mobile Money integration'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'enable-sure-mobile-money-payment'
);

INSERT INTO c_configuration (name, value, date_value, enabled, is_trap_door, description)
SELECT 'enable-yo-payment-mobile-money-payment', NULL, NULL, 1, 0, 'Enable Yo Payment Mobile Money integration'
WHERE NOT EXISTS (
    SELECT 1 FROM c_configuration WHERE name = 'enable-yo-payment-mobile-money-payment'
);
