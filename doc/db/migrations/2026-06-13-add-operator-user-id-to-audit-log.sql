USE `xxl_job`;

SET @db_name = DATABASE();

SET @has_operator_user_id = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_audit_log'
      AND COLUMN_NAME = 'operator_user_id'
);

SET @ddl = IF(
    @has_operator_user_id = 0,
    'ALTER TABLE xxl_job_audit_log ADD COLUMN operator_user_id varchar(64) DEFAULT NULL COMMENT ''操作人用户ID，系统动作可用逻辑ID'' AFTER client_ip',
    'SELECT ''operator_user_id column already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_operator_user_id_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_audit_log'
      AND INDEX_NAME = 'i_operator_user_id'
);

SET @ddl = IF(
    @has_operator_user_id_idx = 0,
    'CREATE INDEX i_operator_user_id ON xxl_job_audit_log (operator_user_id)',
    'SELECT ''i_operator_user_id index already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
