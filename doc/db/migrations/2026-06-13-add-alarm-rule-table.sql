USE `xxl_job`;

SET @db_name = DATABASE();

SET @has_alarm_rule_table = (
    SELECT COUNT(1)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'xxl_job_alarm_rule'
);

SET @ddl = IF(
    @has_alarm_rule_table = 0,
    'CREATE TABLE xxl_job_alarm_rule (
        id int(11) NOT NULL AUTO_INCREMENT,
        name varchar(64) NOT NULL COMMENT ''规则名称'',
        job_group int(11) NOT NULL COMMENT ''执行器主键ID'',
        job_id int(11) DEFAULT NULL COMMENT ''任务ID，为空表示执行器级规则'',
        alarm_event varchar(32) NOT NULL COMMENT ''告警事件类型'',
        channel_ids varchar(255) NOT NULL COMMENT ''绑定渠道ID，多个逗号分隔'',
        enabled tinyint(4) NOT NULL DEFAULT ''1'' COMMENT ''启用状态：0-停用，1-启用'',
        remark varchar(255) DEFAULT NULL COMMENT ''备注'',
        update_time datetime DEFAULT NULL,
        PRIMARY KEY (id),
        KEY i_job_group (job_group),
        KEY i_job_id (job_id),
        KEY i_alarm_event (alarm_event),
        KEY i_enabled (enabled)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4',
    'SELECT ''xxl_job_alarm_rule already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
