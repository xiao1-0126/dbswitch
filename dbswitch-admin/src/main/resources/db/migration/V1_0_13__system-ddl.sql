ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `mode` varchar(20) not null default '0'  comment '连接模式：0默认 1专业' AFTER `driver`;

ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `address` varchar(200) not null default ''  comment '连接地址' AFTER `mode`;

ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `port` varchar(20) not null default ''  comment '连接端口号' AFTER `address`;

ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `database_name` varchar(200) not null default ''  comment '数据库名' AFTER `port`;

ALTER TABLE `DBSWITCH_DATABASE_CONNECTION`
ADD COLUMN `character_encoding` varchar(20) not null default ''  comment '编码格式' AFTER `database_name`;
