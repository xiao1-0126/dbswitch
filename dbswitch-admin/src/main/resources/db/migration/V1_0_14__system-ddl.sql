ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG`
CHANGE COLUMN `excluded` `excluded_flag`  tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否排除(0:否 1:是)' AFTER `source_tables`;
