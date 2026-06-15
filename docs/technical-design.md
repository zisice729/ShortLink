# 短链接系统技术方案

## 一、业务背景

### 1.1 业务需求

随着移动互联网的快速发展，短链接服务已成为互联网基础设施的重要组成部分。短链接服务能够将冗长的URL转换为简短的6-8位字符代码，广泛应用于以下场景：

- **社交媒体分享**：微博、微信等平台有字符限制，短链接节省空间
- **营销推广**：便于统计点击量、用户行为分析
- **短信营销**：降低短信发送成本
- **二维码生成**：短链接生成的二维码更易于扫描

### 1.2 核心挑战

| 挑战 | 说明 |
|------|------|
| **高并发跳转** | 需要支持百万级QPS，跳转响应时间<10ms |
| **短码唯一性** | 全局唯一短码，防止冲突 |
| **防重复生成** | 相同长链接应返回相同短码，减少存储冗余 |
| **缓存穿透** | 防止恶意遍历不存在的短码 |
| **缓存雪崩** | 大量缓存同时过期导致数据库压力 |
| **缓存击穿** | 热点短码缓存失效瞬间大量请求穿透 |
| **恶意链接** | 需要拦截钓鱼、病毒等恶意域名 |
| **域名防劫持** | 防止DNS劫持和跨域盗用 |

### 1.3 业务指标

| 指标 | 目标值 |
|------|--------|
| 跳转QPS | 10万+ |
| 跳转延迟 | <10ms |
| 可用性 | 99.99% |
| 短码长度 | 6-8位 |
| 短码容量 | 568亿（6位）~ 218万亿（8位） |

---

## 二、系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户请求层                                │
│                   (浏览器/移动端/第三方应用)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      接入层 (OpenResty)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Nginx + Lua脚本                                          │   │
│  │  - 布隆过滤器校验                                         │   │
│  │  - Redis直连查询                                          │   │
│  │  - 302重定向                                              │   │
│  │  - 故障降级转发                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
┌─────────────────────┐      ┌─────────────────────┐
│   Redis集群         │      │   SpringBoot服务    │
│  ┌───────────────┐  │      │  ┌───────────────┐  │
│  │ 短链映射缓存  │  │      │  │ 写链路接口    │  │
│  │ 长链去重索引  │  │◄─────┤  │ 降级兜底接口  │  │
│  │ 布隆过滤器    │  │      │  │ 管理接口      │  │
│  │ 访问计数器    │  │      │  └───────────────┘  │
│  └───────────────┘  │      └──────────┬──────────┘
└─────────────────────┘                 │
                                       │
                                       ▼
                              ┌─────────────────────┐
                              │   MySQL主从集群     │
                              │  ┌───────────────┐  │
                              │  │ 短链映射表    │  │
                              │  │ 索引优化      │  │
                              │  └───────────────┘  │
                              └─────────────────────┘
                                       │
                                       ▼
                              ┌─────────────────────┐
                              │   Kafka消息队列     │
                              │  ┌───────────────┐  │
                              │  │ 访问日志Topic │  │
                              │  │ 统计消费      │  │
                              │  └───────────────┘  │
                              └─────────────────────┘
```

### 2.2 技术选型

| 层级 | 技术 | 版本 | 选型理由 |
|------|------|------|----------|
| **接入层** | OpenResty | 1.21+ | 高性能、Lua脚本支持、直接连接Redis |
| **业务层** | SpringBoot | 3.2.0 | 生态成熟、快速开发、易于维护 |
| **缓存层** | Redis | 7.0+ | 高性能KV存储、支持RedisBloom模块 |
| **持久化层** | MySQL | 8.0+ | 成熟稳定、支持事务、适合结构化数据 |
| **消息队列** | Kafka | 3.0+ | 高吞吐量、日志采集、异步统计 |
| **分布式锁** | Redisson | 3.23+ | 基于Redis的分布式锁、支持可重入 |
| **ORM框架** | MyBatis Plus | 3.5.5 | 简化CRUD、支持代码生成 |

### 2.3 核心组件说明

#### 2.3.1 OpenResty接入层

**职责：**
- 接收用户跳转请求
- 布隆过滤器快速校验短码有效性
- 直接查询Redis获取长链接
- 执行302重定向
- Redis故障时降级到Java服务

**优势：**
- 绕过Java应用，减少网络开销
- Lua脚本执行效率高
- 支持百万级QPS

#### 2.3.2 SpringBoot业务层

**职责：**
- 短链生成（写链路）
- 降级兜底（Redis故障时）
- 长链接标准化
- MD5防重校验
- 分布式锁控制

**核心服务：**
- `GenerateService`：短链生成服务
- `FallbackService`：降级兜底服务

#### 2.3.3 Redis缓存层

**数据结构：**

| Key类型 | Key格式 | Value类型 | 说明 |
|---------|---------|-----------|------|
| String | `short:{shortCode}` | Hash | 短链映射（long_url, status, expire_at） |
| String | `long2short:{urlMd5}` | String | 长链反向索引（防重复生成） |
| Set | `short_bloom` | Set | 布隆过滤器（防缓存穿透） |
| String | `short_id_incr` | String | Redis自增ID（短码生成） |

#### 2.3.4 MySQL持久化层

**表结构：**

```sql
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
  UNIQUE KEY `uk_url_md5` (`url_md5`),
  UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='长短链接映射表';
```

**索引策略：**
- `uk_url_md5`：长链唯一索引，防止重复生成
- `uk_short_code`：短码唯一索引，保证全局唯一

---

## 三、核心流程设计

### 3.1 短链生成流程（写链路）

```
┌─────────────┐
│ 用户提交长链 │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 1. 长链接标准化                      │
│    - 域名转小写                      │
│    - 参数排序                        │
│    - 去除末尾/                       │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 2. 计算MD5                          │
│    urlMd5 = MD5(standardUrl)        │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 3. 查询Redis反向索引                 │
│    existCode = Redis.get(           │
│      "long2short:" + urlMd5)        │
└──────┬──────────────────────────────┘
       │
       ▼
    ┌──┴──┐
    │存在?│
    └──┬──┘
  是 │   │ 否
     ▼   ▼
┌─────────┐  ┌─────────────────────────────────────┐
│返回已有│  │ 4. 获取分布式锁                        │
│短链    │  │    lock = Redisson.getLock(          │
└─────────┘  │      "lock:" + urlMd5)             │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 5. 双重检查（防止锁等待期间重复）    │
             │    existCode = Redis.get(...)       │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 6. Redis自增ID                      │
             │    id = Redis.incr("short_id_incr") │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 7. ID转62进制短码                   │
             │    shortCode = Num62Util.numTo62(id)│
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 8. 写入MySQL                        │
             │    INSERT INTO short_link(...)      │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 9. 写入Redis缓存                    │
             │    Redis.hset("short:" + shortCode, │
             │      "long_url", longUrl)           │
             │    Redis.hset(..., "status", 1)     │
             │    Redis.hset(..., "expire_at", ...)│
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 10. 写入反向索引                    │
             │     Redis.set("long2short:" + urlMd5,│
             │       shortCode, 30天)              │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 11. 添加到布隆过滤器                │
             │     Redis.sadd("short_bloom",       │
             │       shortCode)                    │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 12. 释放分布式锁                    │
             │     lock.unlock()                   │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 13. 返回短链接                      │
             │     https://s.xxx.com/{shortCode}   │
             └─────────────────────────────────────┘
```

### 3.2 短链跳转流程（读链路）

```
┌─────────────┐
│ 用户访问短链 │
│ /{shortCode}│
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│ OpenResty接收请求                   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 1. Lua脚本执行                      │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 2. 连接Redis                        │
│    red:connect("127.0.0.1", 6379)   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 3. 布隆过滤器校验                   │
│    exists = red:bf_exists(          │
│      "short_bloom", shortCode)      │
└──────┬──────────────────────────────┘
       │
       ▼
    ┌──┴──┐
    │存在?│
    └──┬──┘
  否 │   │ 是
     ▼   ▼
┌─────────┐  ┌─────────────────────────────────────┐
│返回404  │  │ 4. 查询Redis Hash                   │
│Not Found│  │    res = red:hgetall(               │
└─────────┘  │      "short:" + shortCode)          │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 5. 解析Hash数据                     │
             │    longUrl = res["long_url"]        │
             │    status = res["status"]           │
             │    expireAt = res["expire_at"]      │
             └──────┬──────────────────────────────┘
                    │
                    ▼
             ┌─────────────────────────────────────┐
             │ 6. 状态校验                         │
             │    if (status != 1 ||               │
             │        now > expireAt)              │
             └──────┬──────────────────────────────┘
                    │
                    ▼
                ┌───┴───┐
                │有效?   │
                └───┬───┘
              否 │   │ 是
                 ▼   ▼
           ┌─────────┐  ┌─────────────────────────────────────┐
           │返回410  │  │ 7. 302重定向                        │
           │Gone     │  │    ngx.redirect(longUrl, 302)       │
           └─────────┘  └──────┬──────────────────────────────┘
                              │
                              ▼
                       ┌─────────────────────────────────────┐
                       │ 8. 记录访问日志（JSON格式）          │
                       │    {"time":"...", "code":"...",     │
                       │     "ip":"..."}                     │
                       └─────────────────────────────────────┘
```

### 3.3 降级兜底流程（Redis故障）

```
┌─────────────────────────────────────┐
│ Redis连接失败或查询超时             │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 1. Nginx转发到Java降级接口          │
│    proxy_pass http://127.0.0.1:8080 │
│    /api/link/fallback               │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 2. FallbackController接收请求       │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 3. 查询MySQL                        │
│    data = mapper.selectByShortCode( │
│      shortCode)                     │
└──────┬──────────────────────────────┘
       │
       ▼
    ┌──┴──┐
    │存在?│
    └──┬──┘
  否 │   │ 是
     ▼   ▼
┌─────────┐  ┌─────────────────────────────────────┐
│返回404  │  │ 4. 状态校验                         │
│Not Found│  │    if (status != 1 ||               │
└─────────┘  │        expireAt < now)              │
             └──────┬──────────────────────────────┘
                    │
                    ▼
                ┌───┴───┐
                │有效?   │
                └───┬───┘
              否 │   │ 是
                 ▼   ▼
           ┌─────────┐  ┌─────────────────────────────────────┐
           │返回410  │  │ 5. 回写Redis缓存                    │
           │Gone     │  │    Redis.hset("short:" + shortCode, │
           └─────────┘  │      "long_url", longUrl)           │
                       │    Redis.hset(..., "status", status) │
                       │    Redis.expire(..., expireSeconds)  │
                       └──────┬──────────────────────────────┘
                              │
                              ▼
                       ┌─────────────────────────────────────┐
                       │ 6. 302重定向                        │
                       │    resp.sendRedirect(longUrl)       │
                       └─────────────────────────────────────┘
```

### 3.4 日志统计流程

```
┌─────────────────────────────────────┐
│ Nginx记录访问日志                   │
│ /usr/local/openresty/logs/          │
│   short_access.log (JSON格式)       │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ Filebeat监听日志文件                │
│ 采集JSON格式日志                    │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 推送到Kafka Topic                   │
│ topic: short_link_log               │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ Kafka Consumer消费日志              │
│  - 解析JSON数据                     │
│  - 提取shortCode、ip、time          │
│  - 聚合统计PV/UV                    │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│ 写入统计库/时序库                   │
│ - 短链访问量统计                    │
│ - 用户行为分析                      │
└─────────────────────────────────────┘
```

---

## 四、对外暴露的HTTP接口

### 4.1 短链生成接口

**接口描述：** 创建短链接，支持自定义过期时间

**请求方式：** `POST /api/link/generate`

**请求头：**
```
Content-Type: application/json
```

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| rawUrl | String | 是 | 原始长链接 | `https://www.example.com/test` |
| expireSecond | Long | 否 | 过期时间（秒），默认86400（1天） | `604800` |

**请求示例：**
```json
{
    "rawUrl": "https://www.example.com/test?param1=value1&param2=value2",
    "expireSecond": 604800
}
```

**响应参数：**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| shortUrl | String | 完整短链接 |
| shortCode | String | 短码 |
| expireSecond | Long | 过期时间（秒） |

**响应示例：**
```json
{
    "shortUrl": "https://s.xxx.com/abc123",
    "shortCode": "abc123",
    "expireSecond": 604800
}
```

**错误码：**

| 错误码 | 说明 |
|--------|------|
| 400 | 参数校验失败 |
| 500 | 系统内部错误 |

---

### 4.2 降级跳转接口

**接口描述：** Redis故障时的降级跳转接口

**请求方式：** `GET /api/link/fallback`

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 位置 |
|--------|------|------|------|------|
| code | String | 是 | 短码 | Path |

**请求示例：**
```
GET /api/link/fallback/abc123
```

**响应：**

| 状态码 | 说明 |
|--------|------|
| 302 | 重定向到原始长链接 |
| 404 | 短链接不存在 |
| 410 | 短链接已禁用或已过期 |

---

### 4.3 短链访问接口（OpenResty）

**接口描述：** 用户访问短链接，由OpenResty处理

**请求方式：** `GET /{shortCode}`

**请求示例：**
```
GET /abc123
```

**响应：**

| 状态码 | 说明 |
|--------|------|
| 302 | 重定向到原始长链接 |
| 404 | 短链接不存在 |
| 410 | 短链接已禁用或已过期 |

---

## 五、相关配置

### 5.1 应用配置（application.yml）

```yaml
server:
  port: 8080

spring:
  application:
    name: short-link-service

  # MySQL配置
  datasource:
    url: jdbc:mysql://localhost:3306/db_shortlink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: admin
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      password:
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2

  # Kafka配置
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      retries: 3
      batch-size: 16384
      linger-ms: 1
      buffer-memory: 33554432

# MyBatis Plus配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  mapper-locations: classpath:mybatis/*.xml

# 限流配置
shortlink:
  limit:
    max-qps: 10000
    burst-capacity: 1000
    window-seconds: 1

# 日志配置
logging:
  level:
    com.shortlink: DEBUG
    org.mybatis: DEBUG
```

### 5.2 环境配置（application-prod.yml）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://prod-mysql:3306/db_shortlink?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}

  data:
    redis:
      host: prod-redis
      port: 6379
      password: ${REDIS_PASSWORD}

  kafka:
    bootstrap-servers: prod-kafka:9092

shortlink:
  limit:
    max-qps: 100000
    burst-capacity: 10000

logging:
  level:
    com.shortlink: INFO
```

### 5.3 OpenResty配置（short_link.conf）

```nginx
server {
    listen 80;
    listen 443 ssl;
    server_name s.xxx.com;

    # SSL配置
    ssl_certificate /usr/local/openresty/ssl/s.xxx.com.crt;
    ssl_certificate_key /usr/local/openresty/ssl/s.xxx.com.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256;

    # 日志格式（JSON，供Filebeat采集）
    log_format link_log_json '{"time":"$time_iso8601","code":"$uri","ip":"$remote_addr"}';
    access_log /usr/local/openresty/logs/short_access.log link_log_json;

    # 短链跳转（匹配6-8位字母数字）
    location ~ ^/([0-9a-zA-Z]{6,8})$ {
        content_by_lua_file /usr/local/openresty/lua/link_redirect.lua;
    }

    # 降级转发
    location /fallback/link {
        proxy_pass http://127.0.0.1:8080/api/link/fallback;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 5.4 Redis配置（redis.conf）

```conf
# 基础配置
bind 127.0.0.1
port 6379
timeout 0
tcp-keepalive 300
daemonize yes

# 内存配置
maxmemory 2gb
maxmemory-policy allkeys-lru

# 持久化配置
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# RedisBloom模块
loadmodule /usr/lib/redis/modules/redisbloom.so
```

### 5.5 布隆过滤器初始化

```bash
# 创建布隆过滤器（误判率0.001，容量1亿）
redis-cli BF.RESERVE short_bloom 0.001 100000000

# 初始化短链ID自增键
redis-cli SET short_id_incr 0
```

### 5.6 Filebeat配置（filebeat.yml）

```yaml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /usr/local/openresty/logs/short_access.log
  json.keys_under_root: true
  json.add_error_key: true

output.kafka:
  hosts: ["localhost:9092"]
  topic: short_link_log
  compression: gzip
  max_message_bytes: 1048576

logging.level: info
```

---

## 六、运维监控

### 6.1 核心监控指标

| 指标类型 | 监控项 | 告警阈值 |
|----------|--------|----------|
| **业务指标** | 短链生成QPS | >10000 |
| | 短链跳转QPS | >100000 |
| | 跳转平均延迟 | >50ms |
| | 跳转失败率 | >1% |
| **存储指标** | Redis命中率 | <90% |
| | MySQL慢查询 | >1s |
| | Redis连接池使用率 | >80% |
| **系统指标** | CPU使用率 | >80% |
| | 内存使用率 | >85% |
| | 磁盘使用率 | >90% |

### 6.2 定时任务

| 任务 | 执行频率 | 脚本 |
|------|----------|------|
| 布隆过滤器重建 | 每周 | `script/bloom_rebuild.sh` |
| 过期数据清理 | 每天凌晨 | `script/data_clean.sh` |
| Redis缓存预热 | 每小时 | 自定义脚本 |

### 6.3 日志采集

```
Nginx日志 → Filebeat → Kafka → 消费者 → 统计库
```

---

## 七、安全防护

### 7.1 域名安全

| 措施 | 说明 |
|------|------|
| HTTPS加密 | 全站HTTPS，防止中间人攻击 |
| HSTS | 强制HTTPS访问 |
| SSL证书 | 使用权威机构签发的证书 |

### 7.2 访问控制

| 措施 | 说明 |
|------|------|
| 限流 | 限制单IP访问频率 |
| IP黑名单 | 拦截恶意IP |
| Referer校验 | 防止跨域盗用 |

### 7.3 内容安全

| 措施 | 说明 |
|------|------|
| 恶意域名库 | 拦截钓鱼、病毒域名 |
| 内网地址拦截 | 禁止生成内网IP短链 |
| URL长度限制 | 最大2048字符 |

---

## 八、性能优化

### 8.1 缓存策略

| 策略 | 说明 |
|------|------|
| 布隆过滤器 | 防缓存穿透 |
| 空值缓存 | 短期缓存不存在的短码 |
| 热点数据保护 | 分布式锁防止缓存击穿 |
| TTL随机偏移 | 防缓存雪崩 |

### 8.2 数据库优化

| 优化项 | 说明 |
|--------|------|
| 索引优化 | url_md5、short_code唯一索引 |
| 读写分离 | 主库写、从库读 |
| 分表策略 | 数据量大时按ID哈希分表 |
| 批量删除 | 分批清理过期数据 |

### 8.3 高并发优化

| 优化项 | 说明 |
|--------|------|
| OpenResty直连Redis | 绕过Java应用 |
| Lua脚本执行 | 高性能跳转 |
| 连接池复用 | Redis、MySQL连接池 |
| 异步统计 | 日志异步写入Kafka |

---

## 九、扩展功能

### 9.1 已实现功能

- ✅ 短链生成（支持自定义过期时间）
- ✅ 短链跳转（302重定向）
- ✅ 防重复生成（MD5去重）
- ✅ 布隆过滤器（防缓存穿透）
- ✅ 分布式锁（防并发重复）
- ✅ 降级兜底（Redis故障）
- ✅ 日志采集（Filebeat + Kafka）

### 9.2 待扩展功能

- ⏳ 自定义短码
- ⏳ 短链密码访问
- ⏳ 访问权限控制（IP白名单）
- ⏳ 地域访问限制
- ⏳ UV/PV统计API
- ⏳ 访问趋势分析
- ⏳ 批量生成短链

---

## 十、部署架构

### 10.1 生产环境部署

```
┌─────────────────────────────────────────────────────────────┐
│                        负载均衡层                            │
│                    (SLB / ELB / Nginx)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ OpenResty-1  │  │ OpenResty-2  │  │ OpenResty-3  │
│ (跳转服务)   │  │ (跳转服务)   │  │ (跳转服务)   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ SpringBoot-1 │  │ SpringBoot-2 │  │ SpringBoot-3 │
│ (写链路)     │  │ (写链路)     │  │ (写链路)     │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Redis Master │  │ Redis Slave1 │  │ Redis Slave2 │
│ (主节点)     │  │ (从节点)     │  │ (从节点)     │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ MySQL Master │  │ MySQL Slave1 │  │ MySQL Slave2 │
│ (主库)       │  │ (从库)       │  │ (从库)       │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 10.2 容器化部署（Docker Compose）

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: db_shortlink
    ports:
      - "3306:3306"
    volumes:
      - ./sql:/docker-entrypoint-initdb.d

  redis:
    image: redis:7.0
    command: redis-server --loadmodule /usr/lib/redis/modules/redisbloom.so
    ports:
      - "6379:6379"

  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  shortlink-service:
    build: ./java-service
    ports:
      - "8080:8080"
    depends_on:
      - mysql
      - redis
      - kafka

  openresty:
    image: openresty/openresty:latest
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./openresty-nginx/nginx.conf:/usr/local/openresty/nginx/conf/nginx.conf
      - ./openresty-nginx/conf.d:/usr/local/openresty/nginx/conf.d
      - ./openresty-nginx/lua:/usr/local/openresty/lua
    depends_on:
      - shortlink-service
```

---

## 十一、总结

本短链接系统采用**读写分离架构**，通过OpenResty + Lua脚本实现高性能跳转，SpringBoot处理写链路和降级兜底，Redis + MySQL构建高可用存储，Kafka实现异步日志统计。系统具备以下特点：

### 核心优势

1. **高性能跳转**：OpenResty直连Redis，跳转延迟<10ms，支持百万级QPS
2. **高可用性**：Redis故障自动降级到MySQL，保证服务不中断
3. **防缓存穿透**：布隆过滤器 + 空值缓存双重防护
4. **防重复生成**：MD5去重 + 分布式锁，相同长链接返回相同短码
5. **可扩展性**：支持水平扩展，易于增加节点
6. **安全可靠**：HTTPS加密、恶意域名拦截、内网地址防护

### 技术亮点

- **62进制转换**：动态长度短码，支持海量短链
- **布隆过滤器**：高效拦截无效短码，减轻数据库压力
- **分布式锁**：防止并发重复生成
- **降级兜底**：Redis故障时自动切换到MySQL
- **异步统计**：日志采集不阻塞主流程

### 适用场景

- 社交媒体分享
- 营销推广活动
- 短信营销
- 二维码生成
- 链接统计分析

---

**文档版本：** v1.0  
**最后更新：** 2026-06-15  
**维护人员：** 短链接系统团队