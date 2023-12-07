ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG`
ADD COLUMN `target_auto_increment` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否支持自增' AFTER `target_only_create`;
