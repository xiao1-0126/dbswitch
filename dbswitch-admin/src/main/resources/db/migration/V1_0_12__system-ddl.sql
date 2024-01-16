ALTER TABLE `DBSWITCH_ASSIGNMENT_CONFIG`
ADD COLUMN `channel_size` bigint(20) unsigned not null default 100  comment '通道队列大小' AFTER `batch_size`;
