#
# XXL-JOB Boost 0.9.4 - full installation schema
# Based on the official XXL-JOB 3.4.2 schema.
# Use only for a new deployment. Do not run against an existing database.

CREATE database if NOT EXISTS `xxl_job` default character set utf8mb4 collate utf8mb4_unicode_ci;
use `xxl_job`;

SET NAMES utf8mb4;

## —————————————————————— job group and registry ——————————————————

CREATE TABLE `xxl_job_group`
(
    `id`           int(11)     NOT NULL AUTO_INCREMENT,
    `app_name`     varchar(64) NOT NULL COMMENT '执行器AppName',
    `title`        varchar(64) NOT NULL COMMENT '执行器名称',
    `address_type` tinyint(4)  NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
    `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
    `update_time`  datetime             DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_registry`
(
    `id`                bigint(20)   NOT NULL AUTO_INCREMENT,
    `registry_group`    varchar(50)  NOT NULL,
    `registry_key`      varchar(255) NOT NULL,
    `registry_value`    varchar(255) NOT NULL,
    `update_time`       datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_g_k_v` (`registry_group`, `registry_key`, `registry_value`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

## —————————————————————— job info ——————————————————

CREATE TABLE `xxl_job_info`
(
    `id`                        int(11)      NOT NULL AUTO_INCREMENT,
    `job_group`                 int(11)      NOT NULL COMMENT '执行器主键ID',
    `job_desc`                  varchar(255) NOT NULL,
    `add_time`                  datetime              DEFAULT NULL,
    `update_time`               datetime              DEFAULT NULL,
    `author`                    varchar(64)           DEFAULT NULL COMMENT '作者',
    `job_tag`                   varchar(255)          DEFAULT NULL COMMENT '任务标签，多个逗号分隔',
    `alarm_email`               varchar(255)          DEFAULT NULL COMMENT '报警邮件',
    `alarm_channel_ids`         varchar(255)          DEFAULT NULL COMMENT '告警渠道ID，多个逗号分隔',
    `alarm_event_types`         varchar(255)          DEFAULT NULL COMMENT '告警事件类型，多个逗号分隔：EXECUTOR_FAIL,EXECUTOR_TIMEOUT,TRIGGER_FAIL',
    `schedule_type`             varchar(50)  NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
    `schedule_conf`             varchar(128)          DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
    `misfire_strategy`          varchar(50)  NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
    `executor_route_strategy`   varchar(50)           DEFAULT NULL COMMENT '执行器路由策略',
    `executor_handler`          varchar(255)          DEFAULT NULL COMMENT '任务handler',
    `executor_param`            text                  DEFAULT NULL COMMENT '任务参数',
    `executor_block_strategy`   varchar(50)           DEFAULT NULL COMMENT '阻塞处理策略',
    `executor_timeout`          int(11)      NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
    `executor_fail_retry_count` int(11)      NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `glue_type`                 varchar(50)  NOT NULL COMMENT 'GLUE类型',
    `glue_source`               mediumtext COMMENT 'GLUE源代码',
    `glue_remark`               varchar(128)          DEFAULT NULL COMMENT 'GLUE备注',
    `glue_updatetime`           datetime              DEFAULT NULL COMMENT 'GLUE更新时间',
    `child_jobid`               varchar(255)          DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
    `trigger_status`            tinyint(4)   NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
    `trigger_last_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time`         bigint(13)   NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_alarm_channel`
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

CREATE TABLE `xxl_job_alarm_rule`
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

CREATE TABLE `xxl_job_logglue`
(
    `id`          int(11)      NOT NULL AUTO_INCREMENT,
    `job_id`      int(11)      NOT NULL COMMENT '任务，主键ID',
    `glue_type`   varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
    `glue_source` mediumtext COMMENT 'GLUE源代码',
    `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
    `add_time`    datetime    DEFAULT NULL,
    `update_time` datetime    DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

## —————————————————————— job log and report ——————————————————

CREATE TABLE `xxl_job_log`
(
    `id`                        bigint(20)          NOT NULL AUTO_INCREMENT,
    `job_group`                 int(11)             NOT NULL COMMENT '执行器主键ID',
    `job_id`                    int(11)             NOT NULL COMMENT '任务，主键ID',
    `executor_address`          varchar(255)        DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
    `executor_handler`          varchar(255)        DEFAULT NULL COMMENT '任务handler',
    `executor_param`            text                DEFAULT NULL COMMENT '任务参数',
    `executor_sharding_param`   varchar(20)         DEFAULT NULL COMMENT '任务分片参数，格式如 1/2',
    `executor_fail_retry_count` int(11)             NOT NULL DEFAULT '0' COMMENT '失败重试次数',
    `trigger_time`              datetime            DEFAULT NULL COMMENT '调度-时间',
    `trigger_code`              int(11)             NOT NULL COMMENT '调度-结果',
    `trigger_msg`               text                COMMENT '调度-日志',
    `handle_time`               datetime            DEFAULT NULL COMMENT '执行-时间',
    `handle_code`               int(11)             NOT NULL COMMENT '执行-状态',
    `handle_msg`                text                COMMENT '执行-日志',
    `alarm_status`              tinyint(4)          NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
    PRIMARY KEY (`id`),
    KEY `I_trigger_time` (`trigger_time`),
    KEY `I_handle_code` (`handle_code`),
    KEY `I_jobgroup` (`job_group`),
    KEY `I_jobid` (`job_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_log_report`
(
    `id`            int(11) NOT NULL AUTO_INCREMENT,
    `trigger_day`   datetime         DEFAULT NULL COMMENT '调度-时间',
    `running_count` int(11) NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
    `suc_count`     int(11) NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
    `fail_count`    int(11) NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
    `update_time`   datetime         DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE `xxl_job_alarm_record`
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

CREATE TABLE `xxl_job_audit_log`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `operator`      varchar(64)  NOT NULL COMMENT '操作人',
    `action_type`   varchar(64)  NOT NULL COMMENT '动作类型',
    `resource_type` varchar(64)  NOT NULL COMMENT '资源类型',
    `resource_id`   varchar(128)          DEFAULT NULL COMMENT '资源ID',
    `resource_name` varchar(255)          DEFAULT NULL COMMENT '资源名称',
    `job_group`     int(11)               DEFAULT NULL COMMENT '执行器主键ID，非任务资源可为空',
    `detail_json`   text                  DEFAULT NULL COMMENT '详细快照JSON',
    `request_path`  varchar(255)          DEFAULT NULL COMMENT '请求路径',
    `request_method` varchar(16)          DEFAULT NULL COMMENT '请求方法',
    `source`        varchar(32)           DEFAULT NULL COMMENT '来源：admin-next/legacy-admin/system',
    `client_ip`     varchar(64)           DEFAULT NULL COMMENT '客户端IP',
    `operator_user_id` varchar(64)        DEFAULT NULL COMMENT '操作人用户ID，系统动作可用逻辑ID',
    `create_time`   datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `i_operator` (`operator`),
    KEY `i_action_type` (`action_type`),
    KEY `i_resource_type` (`resource_type`),
    KEY `i_operator_user_id` (`operator_user_id`),
    KEY `i_job_group` (`job_group`),
    KEY `i_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

## —————————————————————— system config ——————————————————

CREATE TABLE `xxl_job_system_config`
(
    `id`           int(11)      NOT NULL AUTO_INCREMENT,
    `config_key`   varchar(100) NOT NULL COMMENT '配置键',
    `config_value` text         NOT NULL COMMENT '配置值',
    `update_time`  datetime              DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

## —————————————————————— lock ——————————————————

CREATE TABLE `xxl_job_lock`
(
    `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY (`lock_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

## —————————————————————— user ——————————————————

CREATE TABLE `xxl_job_user`
(
    `id`         int(11)     NOT NULL AUTO_INCREMENT,
    `username`   varchar(50) NOT NULL COMMENT '账号',
    `password`   varchar(100) NOT NULL COMMENT '密码加密信息',
    `token`      varchar(100) DEFAULT NULL COMMENT '登录token',
    `role`       tinyint(4)  NOT NULL COMMENT '角色：0-普通用户、1-管理员',
    `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
    PRIMARY KEY (`id`),
    UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;


## —————————————————————— for default data ——————————————————

INSERT INTO `xxl_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
    VALUES (1, 'xxl-job-executor-sample', '通用执行器Sample', 0, NULL, now()),
           (2, 'xxl-job-executor-sample-ai', 'AI执行器Sample', 0, NULL, now());

INSERT INTO `xxl_job_info`(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
                           `alarm_channel_ids`, `alarm_event_types`, `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
                           `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
                           `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
                           `child_jobid`)
VALUES (1, 1, '示例任务01', now(), now(), 'XXL', '', '', '', 'CRON', '0 0 0 * * ? *',
        'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), ''),
       (2, 2, 'Ollama示例任务', now(), now(), 'XXL', '', '', '', 'NONE', '',
        'DO_NOTHING', 'FIRST', 'ollamaJobHandler', '{
    "input": "Java实现二叉树层序遍历",
    "prompt": "你是一个研发工程师，擅长解决技术类问题。",
    "model": "qwen3.5:2b"
}', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), ''),
       (3, 2, 'Dify示例任务', now(), now(), 'XXL', '', '', '', 'NONE', '',
        'DO_NOTHING', 'FIRST', 'difyWorkflowJobHandler', '{
    "inputs":{
        "input":"查询班级各学科前三名"
    },
    "user": "xxl-job",
    "baseUrl": "http://localhost/v1",
    "apiKey": "app-OUVgNUOQRIMokfmuJvBJoUTN"
}', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), ''),
       (4, 2, 'OpenClaw示例任务', now(), now(), 'XXL', '', '', '', 'NONE', '',
        'DO_NOTHING', 'FIRST', 'openClawJobHandler', '{
    "input": "查看下上海今天得天气，给出出游建议",
    "prompt": "你是一个出游助手，擅长做旅游规划"
}', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), '');

INSERT INTO `xxl_job_user`(`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL);

INSERT INTO `xxl_job_system_config` (`config_key`, `config_value`, `update_time`)
VALUES ('mail.enabled', 'false', now()),
       ('mail.host', 'smtp.qq.com', now()),
       ('mail.port', '25', now()),
       ('mail.username', '', now()),
       ('mail.from', '', now()),
       ('mail.password', '', now()),
       ('mail.smtp.auth', 'true', now()),
       ('mail.smtp.starttls.enable', 'true', now()),
       ('mail.smtp.starttls.required', 'true', now()),
       ('mail.smtp.ssl.enable', 'false', now());

INSERT INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock');

commit;
