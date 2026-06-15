
CREATE DATABASE IF NOT EXISTS db_shortlink DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE db_shortlink;

CREATE TABLE IF NOT EXISTS `short_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `short_code` varchar(20) NOT NULL COMMENT '短码',
  `long_url` varchar(1024) NOT NULL COMMENT '原始长链接',
  `url_md5` char(32) NOT NULL COMMENT '标准化后链接MD5',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常 0禁用 2已过期',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_md5` (`url_md5`),
  UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长短链接映射表';
