-- Check if the vendor_tran_id column exists and add it if it doesn't
SET @exist := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'm_yo_payment_transaction'
    AND COLUMN_NAME = 'vendor_tran_id'
);

SET @query = IF(@exist = 0, 
    'ALTER TABLE `m_yo_payment_transaction` ADD COLUMN `vendor_tran_id` VARCHAR(100) NULL AFTER `error_msg`', 
    'SELECT "Column vendor_tran_id already exists"'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if middleware_reference index exists
SET @exist_middleware_idx := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'm_yo_payment_transaction'
    AND INDEX_NAME = 'IDX_yo_payment_transaction_middleware_reference'
);

-- Create middleware_reference index if it doesn't exist
SET @query_middleware_idx = IF(@exist_middleware_idx = 0, 
    'CREATE INDEX `IDX_yo_payment_transaction_middleware_reference` ON `m_yo_payment_transaction` (`middleware_reference_no`)', 
    'SELECT "Index IDX_yo_payment_transaction_middleware_reference already exists"'
);

PREPARE stmt FROM @query_middleware_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Check if vendor_tran_id index exists
SET @exist_vendor_idx := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'm_yo_payment_transaction'
    AND INDEX_NAME = 'IDX_yo_payment_transaction_vendor_tran_id'
);

-- Create vendor_tran_id index if it doesn't exist
SET @query_vendor_idx = IF(@exist_vendor_idx = 0, 
    'CREATE INDEX `IDX_yo_payment_transaction_vendor_tran_id` ON `m_yo_payment_transaction` (`vendor_tran_id`)', 
    'SELECT "Index IDX_yo_payment_transaction_vendor_tran_id already exists"'
);

PREPARE stmt FROM @query_vendor_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 