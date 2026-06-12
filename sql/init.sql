
CREATE DATABASE IF NOT EXISTS example_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE example_db;

CREATE TABLE IF NOT EXISTS `short_url` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键（用于生成短码）',
  `short_code` VARCHAR(20) NOT NULL COMMENT '短码（唯一）',
  `long_url` VARCHAR(2048) NOT NULL COMMENT '原始长链接',
  `click_count` BIGINT DEFAULT 0 COMMENT '点击次数',
  `expire_time` DATETIME COMMENT '过期时间（NULL表示永不过期）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0禁用 -1已删除',
  `creator` VARCHAR(64) COMMENT '创建者标识',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_short_code` (`short_code`),
  INDEX `idx_expire_time` (`expire_time`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接映射表';
