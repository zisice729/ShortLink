
# 一、整体项目文件目录
## 1. 目录总览
```
short-link-project/
├── docs/                    # 项目文档、架构、接口文档
├── sql/                     # 数据库脚本、初始化SQL
├── java-service/            # Java 短链生成服务 + 降级兜底接口（SpringBoot）
├── openresty-nginx/         # OpenResty 配置 + Lua 跳转脚本
├── script/                  # 运维脚本（布隆过滤器重建、日志消费、定时任务）
├── config/                  # 全局配置（Redis、Kafka、限流、环境配置）
└── readme.md                # 项目说明、启动流程
```

---

## 2. 分级详细目录
### 2.1 数据库脚本 sql/
```
sql/
├── init_table.sql           # 主表创建、索引、唯一约束
├── init_data.sql            # 初始数据、Redis自增键初始化
└── cron_clean.sql           # 过期数据清理SQL
```

### 2.2 Java 后端服务 java-service/（SpringBoot 标准结构）
```
java-service/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/shortlink/
│   │   │   ├── ShortLinkApplication.java  # 启动类
│   │   │   ├── config/                    # 配置类
│   │   │   │   ├── RedisConfig.java       # Redis、Redisson分布式锁配置
│   │   │   │   ├── KafkaConfig.java       # 消息队列配置
│   │   │   │   └── LimitConfig.java       # 限流配置
│   │   │   ├── controller/                # 接口层
│   │   │   │   ├── GenerateController.java # 短链生成接口（写链路）
│   │   │   │   └── FallbackController.java # 跳转降级兜底接口
│   │   │   ├── service/                   # 业务服务层
│   │   │   │   ├── impl/
│   │   │   │   │   ├── GenerateServiceImpl.java
│   │   │   │   │   └── FallbackServiceImpl.java
│   │   │   │   ├── GenerateService.java
│   │   │   │   └── FallbackService.java
│   │   │   ├── mapper/                    # MyBatis 数据访问层
│   │   │   │   └── ShortLinkMapper.java
│   │   │   ├── entity/                    # 实体类
│   │   │   │   ├── ShortLinkDO.java        # 数据库DO
│   │   │   │   └── dto/
│   │   │   │       ├── GenerateReqDTO.java
│   │   │   │       └── GenerateRespDTO.java
│   │   │   ├── util/                      # 工具类
│   │   │   │   ├── UrlStandardUtil.java    # 长链接标准化工具
│   │   │   │   ├── Md5Util.java            # MD5工具
│   │   │   │   └── Num62Util.java          # 十进制 ↔ 62进制转换工具
│   │   │   └── exception/                  # 全局异常处理
│   │   └── resources/
│   │       ├── application.yml            # 主配置文件
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── mybatis/                   # Mapper XML
│   └── test/                              # 单元测试
└── target/
```

### 2.3 OpenResty(Nginx) 层 openresty-nginx/
```
openresty-nginx/
├── nginx.conf               # Nginx 主配置
├── conf.d/
│   ├── short_link.conf      # 短链跳转站点配置
│   └── fallback.conf        # 转发至Java兜底接口配置
├── lua/
│   ├── link_redirect.lua    # 核心跳转Lua脚本（布隆+Redis查询+降级）
│   └── log_format.lua       # 日志格式化脚本
└── logs/                    # Nginx 运行日志
```

### 2.4 运维&定时脚本 script/
```
script/
├── bloom_rebuild.sh         # 每周重建布隆过滤器脚本
├── data_clean.sh            # 过期数据定时清理脚本
├── filebeat.yml             # Filebeat 采集Nginx日志 → Kafka
└── kafka_consumer.py        # 消费Kafka日志，做PV/UV统计（伪代码）
```

### 2.5 全局配置 config/
```
config/
├── redis.conf               # Redis 基础配置参考
├── redis_bloom.conf         # RedisBloom 布隆过滤器参数
└── env.sh                   # 环境变量、启停脚本
```

---

# 二、核心伪代码 & 脚本
## 1. 数据库初始化 SQL `sql/init_table.sql`
```sql
-- 短链主表
CREATE TABLE `short_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `short_code` varchar(20) NOT NULL COMMENT '短码',
  `long_url` varchar(1024) NOT NULL COMMENT '原始长链接',
  `url_md5` char(32) NOT NULL COMMENT '标准化后链接MD5',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常 0禁用 2已过期',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_url_md5` (`url_md5`),    -- 长链唯一防重
  UNIQUE KEY `uk_short_code` (`short_code`) -- 短码唯一约束
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长短链接映射表';

-- 初始化Redis自增ID键（执行一次）
-- Redis 命令：SET short_id_incr 0
```

## 2. 工具类伪代码（Java）
### 2.1 62进制转换 `Num62Util.java`
支持**动态长度**：ID < 62^6 输出6位，超出自动扩展位数
```java
package com.shortlink.util;

public class Num62Util {
    private static final String CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SCALE = 62;
    private static final long MAX_6BIT = 56800235584L;

    public static String numTo62(long num) {
        if (num <= 0) return "";
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARS.charAt((int) (num % SCALE)));
            num = num / SCALE;
        }
        return sb.reverse().toString();
    }

    public static long codeToNum(String code) {
        long res = 0;
        for (char c : code.toCharArray()) {
            res = res * SCALE + CHARS.indexOf(c);
        }
        return res;
    }

    public static boolean isOver6Bit(long num) {
        return num > MAX_6BIT;
    }
}
```

### 2.2 长链接标准化 `UrlStandardUtil.java`
统一格式，保证相同链接生成同一个MD5
```java
package com.shortlink.util;

public class UrlStandardUtil {
    public static String standard(String url) {
        if (url == null || url.isEmpty()) return "";
        String standardUrl = url.trim().toLowerCase();
        if (standardUrl.endsWith("/")) {
            standardUrl = standardUrl.substring(0, standardUrl.length() - 1);
        }
        return standardUrl;
    }
}
```

## 3. 短链生成 业务层伪代码 `GenerateServiceImpl.java`
核心流程：标准化链接 → MD5防重 → Redis自增ID → 分布式锁 + 双重检查 → 落库 + 写缓存 + 布隆过滤器
```java
package com.shortlink.service.impl;

@Service
public class GenerateServiceImpl implements GenerateService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ShortLinkMapper shortLinkMapper;

    private static final String KEY_LONG_2_SHORT = "long2short:";
    private static final String KEY_SHORT_ID_INCR = "short_id_incr";
    private static final String KEY_LOCK_PREFIX = "lock:";
    private static final String BLOOM_FILTER = "short_bloom";
    private static final int LONG_CACHE_TTL = 30;

    @Override
    public String generateShortUrl(String rawUrl, long expireSecond) {
        String standardUrl = UrlStandardUtil.standard(rawUrl);
        String urlMd5 = Md5Util.md5(standardUrl);

        String existCode = (String) redisTemplate.opsForValue().get(KEY_LONG_2_SHORT + urlMd5);
        if (existCode != null) {
            return buildFullUrl(existCode);
        }

        String lockKey = KEY_LOCK_PREFIX + urlMd5;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock(5, TimeUnit.SECONDS);

            existCode = (String) redisTemplate.opsForValue().get(KEY_LONG_2_SHORT + urlMd5);
            if (existCode != null) {
                return buildFullUrl(existCode);
            }

            Long id = redisTemplate.opsForValue().increment(KEY_SHORT_ID_INCR, 1);
            String shortCode = Num62Util.numTo62(id);

            ShortLinkDO data = new ShortLinkDO();
            data.setShortCode(shortCode);
            data.setLongUrl(rawUrl);
            data.setUrlMd5(urlMd5);
            data.setStatus(1);
            data.setExpireAt(System.currentTimeMillis() + expireSecond * 1000L);
            shortLinkMapper.insert(data);

            String shortKey = "short:" + shortCode;
            redisTemplate.opsForHash().put(shortKey, "long_url", rawUrl);
            redisTemplate.opsForHash().put(shortKey, "status", 1);
            redisTemplate.opsForHash().put(shortKey, "expire_at", data.getExpireAt());
            redisTemplate.expire(shortKey, expireSecond, TimeUnit.SECONDS);

            redisTemplate.opsForValue().set(KEY_LONG_2_SHORT + urlMd5, shortCode, LONG_CACHE_TTL, TimeUnit.DAYS);

            redisTemplate.opsForSet().add(BLOOM_FILTER, shortCode);

            return buildFullUrl(shortCode);
        } finally {
            lock.unlock();
        }
    }

    private String buildFullUrl(String code) {
        return "https://s.xxx.com/" + code;
    }
}
```

## 4. 跳转 Lua 脚本 `openresty-nginx/lua/link_redirect.lua`
OpenResty 核心逻辑：布隆过滤器校验 → Redis查询 → 状态判断 → 降级Java接口
```lua
local redis = require "resty.redis"
local red = redis:new()

local redis_host = "127.0.0.1"
local redis_port = 6379
local timeout = 100

red:set_timeout(timeout)
local ok, err = red:connect(redis_host, redis_port)
if not ok then
    ngx.location.capture("/fallback/link")
    return
end

local short_code = ngx.var[1]
local bloom_key = "short_bloom"
local short_key = "short:" .. short_code

local exists, err = red:bf_exists(bloom_key, short_code)
if exists == 0 then
    ngx.status = 404
    ngx.say("Not Found")
    return
end

local res, err = red:hgetall(short_key)
if not res or #res == 0 then
    ngx.location.capture("/fallback/link")
    return
end

local url = nil
local status = 0
local expire_at = 0
for i = 1, #res, 2 do
    if res[i] == "long_url" then
        url = res[i+1]
    elseif res[i] == "status" then
        status = tonumber(res[i+1])
    elseif res[i] == "expire_at" then
        expire_at = tonumber(res[i+1])
    end
end

local now = ngx.time()
if status ~= 1 or now > expire_at then
    ngx.status = 410
    ngx.say("Gone")
    return
end

ngx.redirect(url, ngx.HTTP_MOVED_TEMPORARILY)
red:set_keepalive(10000, 100)
```

## 5. Nginx 站点配置 `openresty-nginx/conf.d/short_link.conf`
```nginx
server {
    listen 80;
    listen 443 ssl;
    server_name s.xxx.com;

    log_format link_log_json '{"time":"$time_iso8601","code":"$uri","ip":"$remote_addr"}';
    access_log /usr/local/openresty/logs/short_access.log link_log_json;

    location ~ ^/([0-9a-zA-Z]+)$ {
        content_by_lua_file /usr/local/openresty/lua/link_redirect.lua;
    }

    location /fallback/link {
        proxy_pass http://127.0.0.1:8080/api/link/fallback;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 6. Java 降级接口伪代码 `FallbackController.java`
Redis挂掉后，查询MySQL并回写缓存
```java
@RestController
@RequestMapping("/api/link")
public class FallbackController {

    @Resource
    private FallbackService fallbackService;

    @GetMapping("/fallback")
    public void fallbackRedirect(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String uri = req.getRequestURI();
        String shortCode = uri.substring(uri.lastIndexOf("/") + 1);

        ShortLinkDO data = fallbackService.getByShortCode(shortCode);
        if (data == null || data.getStatus() != 1 || System.currentTimeMillis() > data.getExpireAt().getTime()) {
            resp.setStatus(410);
            return;
        }

        fallbackService.refreshCache(data);

        resp.sendRedirect(data.getLongUrl());
    }
}
```

## 7. 定时运维脚本伪代码
### 7.1 布隆过滤器重建 `bloom_rebuild.sh`
每周执行，全量DB数据刷新布隆过滤器
```shell
#!/bin/bash
redis-cli DEL short_bloom
redis-cli BF.RESERVE short_bloom 0.001 100000000

mysql -uroot -p db_shortlink -e "select short_code from short_link where status=1 and expire_at > now()" \
| awk '{print $1}' \
| xargs -I {} redis-cli BF.ADD short_bloom {}
```

### 7.2 过期数据清理 `data_clean.sh`
```shell
#!/bin/bash
mysql -uroot -p db_shortlink << EOF
delete from short_link where status != 1 or expire_at < now() limit 1000;
EOF
```

## 8. 日志采集链路伪逻辑（Filebeat + Kafka）
1. Filebeat 监听 Nginx `short_access.log`，采集JSON日志
2. 推送至 Kafka 指定 Topic
3. 消费端伪代码（Python/Java）做PV/UV统计：
```python
from kafka import KafkaConsumer

consumer = KafkaConsumer('short_link_log')
for msg in consumer:
    log_json = msg.value.decode("utf-8")
    # 解析短码、访问IP、时间
    # 聚合统计 PV(总访问)、UV(独立IP)
    # 写入统计库/时序库
```

---

# 三、整体逻辑回顾（对应你最初架构）
1. **写链路**：用户请求 → 网关校验 → Java服务（标准化URL→MD5→分布式锁→Redis自增ID→62进制→MySQL+Redis+布隆过滤器）
2. **读链路**：用户请求 → OpenResty(Nginx) → Lua脚本（布隆过滤器→Redis查询）→ 302重定向
3. **降级链路**：Redis故障 → Nginx转发 → Java兜底接口（查MySQL + 回写缓存）
4. **统计链路**：Nginx日志 → Filebeat → Kafka → 异步统计PV/UV
5. **运维兜底**：定时清理过期数据、每周重建布隆过滤器
