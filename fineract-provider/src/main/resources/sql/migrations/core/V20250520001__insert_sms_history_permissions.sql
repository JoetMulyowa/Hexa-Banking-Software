-- Add permissions for SMS history feature

-- Get the next permission id
SET @permission_id = (SELECT MAX(id) FROM m_permission) + 1;

-- Add permissions for SMS history
INSERT INTO m_permission (id, grouping, code, entity_name, action_name, can_maker_checker, description)
VALUES
(@permission_id, 'configuration', 'READ_SMS_HISTORY', 'SMS_HISTORY', 'READ', 0, 'Read SMS history'),
(@permission_id + 1, 'configuration', 'CREATE_SMS_HISTORY', 'SMS_HISTORY', 'CREATE', 0, 'Create SMS messages'),
(@permission_id + 2, 'configuration', 'READ_SMS_CONFIGURATION', 'SMS_CONFIGURATION', 'READ', 0, 'View SMS configuration'),
(@permission_id + 3, 'configuration', 'UPDATE_SMS_CONFIGURATION', 'SMS_CONFIGURATION', 'UPDATE', 0, 'Update SMS configuration');

-- Add permissions to roles
INSERT INTO m_role_permission (role_id, permission_id)
SELECT
    r.id as role_id,
    p.id as permission_id
FROM m_role r, m_permission p
WHERE r.name IN ('Super user', 'Administrator')
AND p.code IN ('READ_SMS_HISTORY', 'CREATE_SMS_HISTORY', 'READ_SMS_CONFIGURATION', 'UPDATE_SMS_CONFIGURATION');
