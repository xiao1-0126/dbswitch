ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG`
ADD COLUMN `target_sync_option` varchar(32) NOT NULL DEFAULT 'INSERT_UPDATE_DELETE' COMMENT '同步增删改选项' AFTER `target_auto_increment`,
ADD COLUMN `before_sql_scripts` varchar(4096) DEFAULT NULL COMMENT '目标端写入的前置执行SQL脚本' AFTER `target_sync_option`,
ADD COLUMN `after_sql_scripts` varchar(4096) DEFAULT NULL COMMENT '目标端写入的后置执行SQL脚本' AFTER `before_sql_scripts`;
