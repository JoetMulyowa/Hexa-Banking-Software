-- SQL queries for SMS history information

-- 1. Show all SMS messages in the system
SELECT * FROM m_sms_outbound_message ORDER BY submitted_on_date DESC LIMIT 100;

-- 2. Show messages for a specific phone number
SELECT * FROM m_sms_outbound_message
WHERE recipient = '256703383908'
ORDER BY submitted_on_date DESC;

-- 3. Show messages for a specific loan
SELECT m.*, l.loan_status_id, l.account_no as loan_account_no, c.display_name as client_name
FROM m_sms_outbound_message m
JOIN m_loan l ON m.reference_entity_id = l.id
JOIN m_client c ON l.client_id = c.id
WHERE m.reference_entity_type = 'LOAN'
ORDER BY m.submitted_on_date DESC
LIMIT 100;

-- 4. Show message delivery statistics
SELECT
    DATE(submitted_on_date) as date,
    COUNT(*) as total_messages,
    SUM(CASE WHEN status_enum = 200 THEN 1 ELSE 0 END) as delivered,
    SUM(CASE WHEN status_enum = 300 THEN 1 ELSE 0 END) as failed,
    SUM(CASE WHEN status_enum = 100 THEN 1 ELSE 0 END) as pending
FROM m_sms_outbound_message
GROUP BY DATE(submitted_on_date)
ORDER BY DATE(submitted_on_date) DESC;

-- 5. Show all error messages
SELECT
    recipient,
    message,
    error_message,
    submitted_on_date
FROM m_sms_outbound_message
WHERE status_enum = 300
ORDER BY submitted_on_date DESC;

-- 6. Show SMS costs by date
SELECT
    DATE(submitted_on_date) as date,
    COUNT(*) as total_messages,
    SUM(CASE WHEN message_cost IS NOT NULL THEN REPLACE(REPLACE(message_cost, 'UGX ', ''), '.0000', '') ELSE 0 END) as total_cost_ugx
FROM m_sms_outbound_message
WHERE status_enum = 200
GROUP BY DATE(submitted_on_date)
ORDER BY DATE(submitted_on_date) DESC;

-- 7. Show message types distribution
SELECT
    sms_type,
    COUNT(*) as total_messages,
    SUM(CASE WHEN status_enum = 200 THEN 1 ELSE 0 END) as delivered,
    SUM(CASE WHEN status_enum = 300 THEN 1 ELSE 0 END) as failed
FROM m_sms_outbound_message
GROUP BY sms_type
ORDER BY total_messages DESC;
