USE `xxl_job`;

SET @db_name = DATABASE();

CREATE TABLE IF NOT EXISTS `xxl_job_audit_log`
(
    `id`             bigint(20)   NOT NULL AUTO_INCREMENT,
    `operator`       varchar(64)  NOT NULL COMMENT '操作人',
    `action_type`    varchar(64)  NOT NULL COMMENT '动作类型',
    `resource_type`  varchar(64)  NOT NULL COMMENT '资源类型',
    `resource_id`    varchar(128)          DEFAULT NULL COMMENT '资源ID',
    `resource_name`  varchar(255)          DEFAULT NULL COMMENT '资源名称',
    `job_group`      int(11)               DEFAULT NULL COMMENT '执行器主键ID，非任务资源可为空',
    `detail_json`    text                  DEFAULT NULL COMMENT '详细快照JSON',
    `request_path`   varchar(255)          DEFAULT NULL COMMENT '请求路径',
    `request_method` varchar(16)           DEFAULT NULL COMMENT '请求方法',
    `source`         varchar(32)           DEFAULT NULL COMMENT '来源：admin-next/legacy-admin/system',
    `client_ip`      varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `create_time`    datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `i_operator` (`operator`),
    KEY `i_action_type` (`action_type`),
    KEY `i_resource_type` (`resource_type`),
    KEY `i_job_group` (`job_group`),
    KEY `i_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

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
