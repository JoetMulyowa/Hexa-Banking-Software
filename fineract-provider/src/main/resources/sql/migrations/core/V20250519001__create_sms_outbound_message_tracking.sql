-- Create table for SMS outbound messages
CREATE TABLE m_sms_outbound_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(100),
    recipient VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    status_enum INT NOT NULL DEFAULT 100,
    error_message TEXT,
    campaign_name VARCHAR(100),
    submitted_on_date DATETIME NOT NULL,
    delivered_on_date DATETIME,
    provider VARCHAR(50) DEFAULT 'AFRICASTALKING',
    message_id VARCHAR(100),
    reference_id BIGINT,
    reference_type VARCHAR(50),
    sms_type VARCHAR(50),
    message_cost VARCHAR(20),
    status_code VARCHAR(10),
    message_parts INT,
    reference_entity_type VARCHAR(50),
    reference_entity_id BIGINT,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    last_modified_by BIGINT,
    CONSTRAINT FK_sms_created_by FOREIGN KEY (created_by) REFERENCES m_appuser(id),
    CONSTRAINT FK_sms_modified_by FOREIGN KEY (last_modified_by) REFERENCES m_appuser(id)
);

-- Create indices for better query performance
CREATE INDEX idx_sms_recipient ON m_sms_outbound_message(recipient);
CREATE INDEX idx_sms_status ON m_sms_outbound_message(status_enum);
CREATE INDEX idx_sms_reference ON m_sms_outbound_message(reference_entity_type, reference_entity_id);
CREATE INDEX idx_sms_external_id ON m_sms_outbound_message(external_id);
CREATE INDEX idx_sms_submitted_date ON m_sms_outbound_message(submitted_on_date);
