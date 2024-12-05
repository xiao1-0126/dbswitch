CREATE TABLE IF NOT EXISTS `DBSWITCH_JOB_LOGBACK` (
  `id`          bigint          not null auto_increment                 comment '自增id',
  `uuid`        varchar(128)    not null                                comment 'job id',
  `content`     longtext                                                comment '日志内容',
  `create_time` timestamp       not null default current_timestamp      comment '创建时间',
  PRIMARY KEY (`id`),
  KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='JOB执行日志';

ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG`
ADD COLUMN `target_only_create` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否只建表' AFTER `target_drop_table`,
ADD COLUMN `table_type` VARCHAR(32) NULL DEFAULT 'TABLE' COMMENT '表类型:TABLE;VIEW' AFTER `source_schema`,
ADD COLUMN `table_name_case`  varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '表名大小写转换策略' AFTER `target_schema`,
ADD COLUMN `column_name_case`  varchar(32) NOT NULL DEFAULT 'NONE' COMMENT '列名大小写转换策略' AFTER `table_name_case`,
ADD COLUMN `target_auto_increment` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否支持自增' AFTER `target_only_create`,
ADD COLUMN `target_sync_option` varchar(32) NOT NULL DEFAULT 'INSERT_UPDATE_DELETE' COMMENT '同步增删改选项' AFTER `target_auto_increment`,
ADD COLUMN `before_sql_scripts` varchar(4096) DEFAULT NULL COMMENT '目标端写入的前置执行SQL脚本' AFTER `target_sync_option`,
ADD COLUMN `after_sql_scripts` varchar(4096) DEFAULT NULL COMMENT '目标端写入的后置执行SQL脚本' AFTER `before_sql_scripts`,
ADD COLUMN `channel_size` bigint(20) unsigned not null default 100  comment '通道队列大小' AFTER `batch_size`;

ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `mode` varchar(20) not null default '0'  comment '连接模式：0默认 1专业' AFTER `driver`,
ADD COLUMN `address` varchar(200) not null default ''  comment '连接地址' AFTER `mode`,
ADD COLUMN `port` varchar(20) not null default ''  comment '连接端口号' AFTER `address`,
ADD COLUMN `database_name` varchar(200) not null default ''  comment '数据库名' AFTER `port`,
ADD COLUMN `character_encoding` varchar(20) not null default ''  comment '编码格式' AFTER `database_name`;

