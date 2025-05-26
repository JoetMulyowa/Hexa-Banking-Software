-- Create a dedicated table for Yo Payment repayment transactions
CREATE TABLE IF NOT EXISTS `m_yo_payment_repayment_transaction` (
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
  `createdby_id` BIGINT NOT NULL,
  `created_date` DATETIME NOT NULL,
  `lastmodifiedby_id` BIGINT NULL,
  `lastmodified_date` DATETIME NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_yo_payment_repayment_transaction_loan` FOREIGN KEY (`loan_id`) REFERENCES `m_loan` (`id`),
  CONSTRAINT `FK_yo_payment_repayment_transaction_loan_transaction` FOREIGN KEY (`loan_transaction_id`) REFERENCES `m_loan_transaction` (`id`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

-- Drop existing indexes if they exist
DROP INDEX IF EXISTS `IDX_yo_payment_repayment_transaction_loan_id` ON `m_yo_payment_repayment_transaction`;
DROP INDEX IF EXISTS `IDX_yo_payment_repayment_transaction_middleware_reference` ON `m_yo_payment_repayment_transaction`;
DROP INDEX IF EXISTS `IDX_yo_payment_repayment_transaction_vendor_tran_id` ON `m_yo_payment_repayment_transaction`;

-- Create an index for faster lookups
CREATE INDEX `IDX_yo_payment_repayment_transaction_loan_id` ON `m_yo_payment_repayment_transaction` (`loan_id`);
CREATE INDEX `IDX_yo_payment_repayment_transaction_middleware_reference` ON `m_yo_payment_repayment_transaction` (`middleware_reference_no`);
CREATE INDEX `IDX_yo_payment_repayment_transaction_vendor_tran_id` ON `m_yo_payment_repayment_transaction` (`vendor_tran_id`); 