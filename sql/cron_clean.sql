
USE db_shortlink;

-- 分批删除已过期+已禁用数据，避免大事务
DELETE FROM short_link 
WHERE (status != 1 OR expire_at < NOW()) 
LIMIT 1000;
