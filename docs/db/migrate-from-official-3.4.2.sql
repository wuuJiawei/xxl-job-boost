-- XXL-JOB official 3.4.2 -> XXL-JOB Boost 1.0.0
--
-- Scope:
--   * Run against an existing official XXL-JOB 3.4.2 database.
--   * Preserve all official tables and business data.
--   * Add only the fields and tables required by XXL-JOB Boost.
--
-- Before running:
--   1. Stop every xxl-job-admin instance.
--   2. Back up the xxl_job database.
--   3. Verify that the selected database is the intended production copy.
--
-- This script is idempotent and can be rerun after a partial failure.

USE `xxl_job`;

SET NAMES utf8mb4;
SET @db_name = DATABASE();

-- Guard against accidentally running against an unrelated database.
SET @has_xxl_job_info = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_info'
);
SET @ddl = IF(
    @has_xxl_job_info = 1,
    'SELECT ''official XXL-JOB schema detected''',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''xxl_job_info is missing; aborting XXL-JOB Boost migration'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Boost metadata on official job definitions.
SET @has_job_tag = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_info'
      AND COLUMN_NAME = 'job_tag'
);
SET @ddl = IF(
    @has_job_tag = 0,
    'ALTER TABLE `xxl_job_info` ADD COLUMN `job_tag` varchar(255) DEFAULT NULL COMMENT ''任务标签，多个逗号分隔'' AFTER `author`',
    'SELECT ''xxl_job_info.job_tag already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_alarm_channel_ids = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_info'
      AND COLUMN_NAME = 'alarm_channel_ids'
);
SET @ddl = IF(
    @has_alarm_channel_ids = 0,
    'ALTER TABLE `xxl_job_info` ADD COLUMN `alarm_channel_ids` varchar(255) DEFAULT NULL COMMENT ''告警渠道ID，多个逗号分隔'' AFTER `alarm_email`',
    'SELECT ''xxl_job_info.alarm_channel_ids already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_alarm_event_types = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_info'
      AND COLUMN_NAME = 'alarm_event_types'
);
SET @ddl = IF(
    @has_alarm_event_types = 0,
    'ALTER TABLE `xxl_job_info` ADD COLUMN `alarm_event_types` varchar(255) DEFAULT NULL COMMENT ''告警事件类型，多个逗号分隔：EXECUTOR_FAIL,EXECUTOR_TIMEOUT,TRIGGER_FAIL'' AFTER `alarm_channel_ids`',
    'SELECT ''xxl_job_info.alarm_event_types already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Boost alarm configuration and delivery history.
CREATE TABLE IF NOT EXISTS `xxl_job_alarm_channel`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT,
    `name`         varchar(64)  NOT NULL COMMENT '渠道名称',
    `type`         varchar(32)  NOT NULL COMMENT '渠道类型：EMAIL/WEBHOOK/FEISHU/WECOM/DINGTALK',
    `endpoint`     varchar(512)          DEFAULT NULL COMMENT 'Webhook地址',
    `recipients`   varchar(512)          DEFAULT NULL COMMENT '邮件接收人，多个逗号分隔',
    `secret`       varchar(255)          DEFAULT NULL COMMENT '预留密钥字段',
    `headers_json` text                  DEFAULT NULL COMMENT '自定义请求头JSON',
    `enabled`      tinyint(4)   NOT NULL DEFAULT '1' COMMENT '启用状态：0-停用，1-启用',
    `remark`       varchar(255)          DEFAULT NULL COMMENT '备注',
    `update_time`  datetime              DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_alarm_rule`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `name`        varchar(64)  NOT NULL COMMENT '规则名称',
    `job_group`   int(11)      NOT NULL COMMENT '执行器主键ID',
    `job_id`      int(11)               DEFAULT NULL COMMENT '任务ID，为空表示执行器级规则',
    `alarm_event` varchar(32)  NOT NULL COMMENT '告警事件类型',
    `channel_ids` varchar(255) NOT NULL COMMENT '绑定渠道ID，多个逗号分隔',
    `enabled`     tinyint(4)   NOT NULL DEFAULT '1' COMMENT '启用状态：0-停用，1-启用',
    `remark`      varchar(255)          DEFAULT NULL COMMENT '备注',
    `update_time` datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `i_job_group` (`job_group`),
    KEY `i_job_id` (`job_id`),
    KEY `i_alarm_event` (`alarm_event`),
    KEY `i_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `xxl_job_alarm_record`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `job_group`     int(11)      NOT NULL COMMENT '执行器主键ID',
    `job_id`        int(11)      NOT NULL COMMENT '任务主键ID',
    `job_log_id`    bigint(20)   NOT NULL COMMENT '任务日志ID',
    `job_desc`      varchar(255)          DEFAULT NULL COMMENT '任务描述',
    `channel_id`    int(11)               DEFAULT NULL COMMENT '渠道ID，legacy邮件可为空',
    `channel_name`  varchar(64)  NOT NULL COMMENT '渠道名称',
    `channel_type`  varchar(32)  NOT NULL COMMENT '渠道类型',
    `alarm_event`   varchar(32)  NOT NULL COMMENT '告警事件类型',
    `target`        varchar(512)          DEFAULT NULL COMMENT '目标地址或接收人',
    `alarm_title`   varchar(255) NOT NULL COMMENT '告警标题',
    `alarm_content` text                  DEFAULT NULL COMMENT '告警内容',
    `send_status`   tinyint(4)   NOT NULL DEFAULT '0' COMMENT '发送状态：1-成功，2-失败',
    `response_code` int(11)               DEFAULT NULL COMMENT '响应状态码',
    `response_body` text                  DEFAULT NULL COMMENT '响应体',
    `error_msg`     varchar(512)          DEFAULT NULL COMMENT '错误信息',
    `create_time`   datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `i_job_group` (`job_group`),
    KEY `i_job_id` (`job_id`),
    KEY `i_job_log_id` (`job_log_id`),
    KEY `i_channel_type` (`channel_type`),
    KEY `i_send_status` (`send_status`),
    KEY `i_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- Compatibility with databases that previously ran an early Boost migration.
SET @has_alarm_event = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND COLUMN_NAME = 'alarm_event'
);
SET @ddl = IF(
    @has_alarm_event = 0,
    'ALTER TABLE `xxl_job_alarm_record` ADD COLUMN `alarm_event` varchar(32) NOT NULL DEFAULT ''EXECUTOR_FAIL'' COMMENT ''告警事件类型'' AFTER `channel_type`',
    'SELECT ''xxl_job_alarm_record.alarm_event already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Boost audit trail.
CREATE TABLE IF NOT EXISTS `xxl_job_audit_log`
(
    `id`               bigint(20)   NOT NULL AUTO_INCREMENT,
    `operator`         varchar(64)  NOT NULL COMMENT '操作人',
    `action_type`      varchar(64)  NOT NULL COMMENT '动作类型',
    `resource_type`    varchar(64)  NOT NULL COMMENT '资源类型',
    `resource_id`      varchar(128)          DEFAULT NULL COMMENT '资源ID',
    `resource_name`    varchar(255)          DEFAULT NULL COMMENT '资源名称',
    `job_group`        int(11)               DEFAULT NULL COMMENT '执行器主键ID，非任务资源可为空',
    `detail_json`      text                  DEFAULT NULL COMMENT '详细快照JSON',
    `request_path`     varchar(255)          DEFAULT NULL COMMENT '请求路径',
    `request_method`   varchar(16)           DEFAULT NULL COMMENT '请求方法',
    `source`           varchar(32)           DEFAULT NULL COMMENT '来源：admin-next/legacy-admin/system',
    `client_ip`        varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `operator_user_id` varchar(64)           DEFAULT NULL COMMENT '操作人用户ID，系统动作可用逻辑ID',
    `create_time`      datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `i_operator` (`operator`),
    KEY `i_action_type` (`action_type`),
    KEY `i_resource_type` (`resource_type`),
    KEY `i_operator_user_id` (`operator_user_id`),
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
    'ALTER TABLE `xxl_job_audit_log` ADD COLUMN `operator_user_id` varchar(64) DEFAULT NULL COMMENT ''操作人用户ID，系统动作可用逻辑ID'' AFTER `client_ip`',
    'SELECT ''xxl_job_audit_log.operator_user_id already exists'''
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
    'CREATE INDEX `i_operator_user_id` ON `xxl_job_audit_log` (`operator_user_id`)',
    'SELECT ''xxl_job_audit_log.i_operator_user_id already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'XXL-JOB official 3.4.2 -> XXL-JOB Boost database migration completed' AS migration_result;
