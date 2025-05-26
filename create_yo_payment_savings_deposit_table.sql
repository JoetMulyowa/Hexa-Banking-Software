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

-- Create table for tracking YoPayment savings deposit transactions
CREATE TABLE IF NOT EXISTS m_yo_payment_savings_deposit_transaction (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  savings_account_id BIGINT NOT NULL,
  savings_transaction_id BIGINT,
  vendor_tran_id VARCHAR(100),
  middleware_reference_no VARCHAR(100),
  transaction_date DATETIME NOT NULL,
  amount DECIMAL(19,6) NOT NULL,
  is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
  status_code VARCHAR(10),
  status_desc VARCHAR(100),
  mobile_number VARCHAR(20) NOT NULL,
  request_body TEXT,
  response_body TEXT,
  error_msg VARCHAR(500),
  createdby_id BIGINT NOT NULL,
  created_date DATETIME NOT NULL,
  lastmodifiedby_id BIGINT,
  lastmodified_date DATETIME,
  CONSTRAINT FK_yo_payment_savings_deposit_transaction_account FOREIGN KEY (savings_account_id) REFERENCES m_savings_account(id),
  CONSTRAINT FK_yo_payment_savings_deposit_transaction FOREIGN KEY (savings_transaction_id) REFERENCES m_savings_account_transaction(id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS IDX_yo_payment_savings_deposit_transaction_account_id
ON m_yo_payment_savings_deposit_transaction (savings_account_id);

CREATE INDEX IF NOT EXISTS IDX_yo_payment_savings_deposit_transaction_middleware_reference
ON m_yo_payment_savings_deposit_transaction (middleware_reference_no);

CREATE INDEX IF NOT EXISTS IDX_yo_payment_savings_deposit_transaction_vendor_tran_id
ON m_yo_payment_savings_deposit_transaction (vendor_tran_id);

-- Add global configuration for YO PAYMENT savings deposit
INSERT INTO c_configuration (name, value, description, enabled, is_trap_door)
SELECT 'enable-yo-payment-savings-deposit', false, 'Enable Yo Payment mobile money integration for savings account deposits', false, false
WHERE NOT EXISTS (SELECT 1 FROM c_configuration WHERE name = 'enable-yo-payment-savings-deposit'); 
