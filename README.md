
# 短链接系统

## 项目简介

本项目是一个高性能短链接系统，基于 OpenResty(Nginx) + SpringBoot + Redis + MySQL 技术栈构建，支持百万级 QPS 跳转请求。

## 技术架构

### 架构图

```
用户请求
    │
    ▼
┌─────────────────┐
│  OpenResty      │ ← Lua脚本处理跳转
│  (Nginx)        │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌─────────┐
│ Redis │ │  Spring │
│(缓存) │ │  Boot   │ ← 写链路、降级兜底
└───────┘ └────┬────┘
               │
               ▼
          ┌─────────┐
          │ MySQL   │ ← 持久化存储
          └─────────┘
```

### 技术栈

| 组件 | 用途 |
|------|------|
| OpenResty | 高性能跳转、Lua脚本 |
| SpringBoot | 短链生成、降级兜底 |
| Redis | 缓存、布隆过滤器 |
| MySQL | 持久化存储 |
| Kafka | 日志采集、统计 |

## 项目结构

```
short-link-project/
├── docs/                    # 项目文档
├── sql/                     # 数据库脚本
├── java-service/            # Java服务
├── openresty-nginx/         # OpenResty配置
├── script/                  # 运维脚本
├── config/                  # 全局配置
└── readme.md                # 项目说明
```

## 快速开始

### 1. 环境要求

- Java 21+
- MySQL 8.0+
- Redis 7.0+ (需安装 RedisBloom 模块)
- OpenResty 1.21+
- Kafka 3.0+ (可选，用于统计)

### 2. 数据库初始化

```bash
mysql -uroot -p < sql/init_table.sql
mysql -uroot -p < sql/init_data.sql
```

### 3. Redis初始化

```bash
redis-cli SET short_id_incr 0
redis-cli BF.RESERVE short_bloom 0.001 100000000
```

### 4. 启动Java服务

```bash
cd java-service
mvn clean package
java -jar target/short-link-service-1.0.0.jar
```

### 5. 启动OpenResty

```bash
cd openresty-nginx
openresty -p . -c nginx.conf
```

## API接口

### 创建短链接

```bash
POST /api/link/generate
Content-Type: application/json

{
    "rawUrl": "https://www.example.com/test",
    "expireSecond": 86400
}
```

### 降级跳转

```bash
GET /api/link/fallback?code=abc123
```

## 运维脚本

### 布隆过滤器重建

```bash
sh script/bloom_rebuild.sh
```

### 过期数据清理

```bash
sh script/data_clean.sh
```

## 配置说明

### 限流配置

```yaml
shortlink:
  limit:
    max-qps: 10000      # 最大QPS
    burst-capacity: 1000 # 突发容量
    window-seconds: 1    # 时间窗口(秒)
```

## 核心流程

### 写链路

```
用户请求 → 网关校验 → Java服务
    → 标准化URL → MD5防重 → 分布式锁
    → Redis自增ID → 62进制转换 → MySQL+Redis+布隆过滤器
```

### 读链路

```
用户请求 → OpenResty → Lua脚本
    → 布隆过滤器校验 → Redis查询 → 302重定向
```

### 降级链路

```
Redis故障 → Nginx转发 → Java兜底接口
    → 查MySQL → 回写缓存 → 302重定向
```
