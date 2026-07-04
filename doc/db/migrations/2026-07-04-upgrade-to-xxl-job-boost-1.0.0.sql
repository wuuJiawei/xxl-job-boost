USE `xxl_job`;

SET @db_name = DATABASE();

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
    'SELECT ''job_tag column already exists'''
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
    'SELECT ''alarm_channel_ids column already exists'''
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
    'SELECT ''alarm_event_types column already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
    `alarm_event`   varchar(32)  NOT NULL DEFAULT 'EXECUTOR_FAIL' COMMENT '告警事件类型',
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
    'SELECT ''alarm_event column already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_job_group_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_job_group'
);
SET @ddl = IF(
    @has_record_job_group_idx = 0,
    'CREATE INDEX `i_job_group` ON `xxl_job_alarm_record` (`job_group`)',
    'SELECT ''xxl_job_alarm_record.i_job_group already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_job_id_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_job_id'
);
SET @ddl = IF(
    @has_record_job_id_idx = 0,
    'CREATE INDEX `i_job_id` ON `xxl_job_alarm_record` (`job_id`)',
    'SELECT ''xxl_job_alarm_record.i_job_id already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_log_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_job_log_id'
);
SET @ddl = IF(
    @has_record_log_idx = 0,
    'CREATE INDEX `i_job_log_id` ON `xxl_job_alarm_record` (`job_log_id`)',
    'SELECT ''xxl_job_alarm_record.i_job_log_id already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_channel_type_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_channel_type'
);
SET @ddl = IF(
    @has_record_channel_type_idx = 0,
    'CREATE INDEX `i_channel_type` ON `xxl_job_alarm_record` (`channel_type`)',
    'SELECT ''xxl_job_alarm_record.i_channel_type already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_send_status_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_send_status'
);
SET @ddl = IF(
    @has_record_send_status_idx = 0,
    'CREATE INDEX `i_send_status` ON `xxl_job_alarm_record` (`send_status`)',
    'SELECT ''xxl_job_alarm_record.i_send_status already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record_create_time_idx = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_record'
      AND INDEX_NAME = 'i_create_time'
);
SET @ddl = IF(
    @has_record_create_time_idx = 0,
    'CREATE INDEX `i_create_time` ON `xxl_job_alarm_record` (`create_time`)',
    'SELECT ''xxl_job_alarm_record.i_create_time already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

