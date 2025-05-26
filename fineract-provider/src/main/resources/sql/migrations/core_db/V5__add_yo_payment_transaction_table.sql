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

-- Create a dedicated table for Yo Payment transactions
CREATE TABLE IF NOT EXISTS `m_yo_payment_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `loan_id` BIGINT NOT NULL,
  `loan_transaction_id` BIGINT NULL,
  `vendor_tran_id` VARCHAR(100) NULL,
  `middleware_reference_no` VARCHAR(100) NULL,
  `transaction_date` DATE NOT NULL,
  `amount` DECIMAL(19,6) NOT NULL,
  `is_reversed` TINYINT NOT NULL DEFAULT 0,
  `status_code` VARCHAR(10) NULL,
  `status_desc` VARCHAR(100) NULL,
  `request_body` TEXT NULL,
  `response_body` TEXT NULL,
  `error_msg` VARCHAR(500) NULL,
  `created_by` BIGINT NOT NULL,
  `created_on_utc` DATETIME NOT NULL,
  `last_modified_by` BIGINT NULL,
  `last_modified_on_utc` DATETIME NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_yo_payment_transaction_loan` FOREIGN KEY (`loan_id`) REFERENCES `m_loan` (`id`),
  CONSTRAINT `FK_yo_payment_transaction_loan_transaction` FOREIGN KEY (`loan_transaction_id`) REFERENCES `m_loan_transaction` (`id`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- Create an index for faster lookups
CREATE INDEX `IDX_yo_payment_transaction_loan_id` ON `m_yo_payment_transaction` (`loan_id`);
CREATE INDEX `IDX_yo_payment_transaction_middleware_reference` ON `m_yo_payment_transaction` (`middleware_reference_no`);
CREATE INDEX `IDX_yo_payment_transaction_vendor_tran_id` ON `m_yo_payment_transaction` (`vendor_tran_id`);
