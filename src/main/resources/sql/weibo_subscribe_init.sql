
-- DROP TABLE IF EXISTS `user_subscribe`;
CREATE TABLE IF NOT EXISTS `user_subscribe` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
    `nickname` VARCHAR(32) DEFAULT NULL COMMENT '用户名',
    `open_flag` int(4) DEFAULT '0' COMMENT '是否启用(0=否,1=是)',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅';


-- DROP TABLE IF EXISTS `history_post`;
CREATE TABLE IF NOT EXISTS `history_post` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` VARCHAR(32) DEFAULT NULL COMMENT '用户ID',
    `nickname` VARCHAR(32) DEFAULT NULL COMMENT '用户名',
    `post_id` VARCHAR(32) DEFAULT NULL COMMENT '文章ID',
    `top_flag` int(4) DEFAULT '0' COMMENT '是否置顶(0=否,1=是)',
    `created_at` datetime DEFAULT NULL COMMENT '文章创建时间',
    `edit_at` datetime DEFAULT NULL COMMENT '文章编辑时间',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文章历史';

