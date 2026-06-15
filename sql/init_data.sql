
USE db_shortlink;

-- 初始化Redis自增ID键（执行一次）
-- Redis 命令：SET short_id_incr 0

-- 初始化测试数据
INSERT INTO `short_link` (`short_code`, `long_url`, `url_md5`, `status`, `expire_at`)
VALUES 
('abc123', 'https://www.example.com/test', MD5('https://www.example.com/test'), 1, DATE_ADD(NOW(), INTERVAL 30 DAY)),
('def456', 'https://www.google.com', MD5('https://www.google.com'), 1, DATE_ADD(NOW(), INTERVAL 7 DAY));
