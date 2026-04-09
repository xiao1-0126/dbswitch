ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG` ADD COLUMN `custom_ddl_map`  longtext NULL COMMENT '自定义建表DDL映射(JSON格式: {\"tableName\":\"ddl...\"})' AFTER `post_sql_scripts`;
