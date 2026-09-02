-- XXL-JOB Boost existing database -> mandatory password security policy
-- Run once against an existing Boost database before starting the upgraded admin.

USE `xxl_job`;

SET NAMES utf8mb4;
SET @db_name = DATABASE();
SET @has_password_change_required = (
    SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'xxl_job_user' AND COLUMN_NAME = 'password_change_required'
);
SET @ddl = IF(
    @has_password_change_required = 0,
    'ALTER TABLE `xxl_job_user` ADD COLUMN `password_change_required` tinyint(1) NOT NULL DEFAULT ''1'' COMMENT ''首次登录必须修改密码'' AFTER `password`',
    'SELECT ''xxl_job_user.password_change_required already exists'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(1) AS users_requiring_password_change
FROM `xxl_job_user`
WHERE `password_change_required` = 1;
