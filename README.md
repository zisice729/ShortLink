
# 短链接系统

基于 Spring Boot 实现的高性能短链接系统，支持高并发跳转、短码全局唯一、过期管理等核心功能。

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.x
- **MyBatis Plus**: 3.5.x
- **Redis**: 7.0+（缓存 + 布隆过滤器）
- **MySQL**: 8.0+（持久化存储）
- **Nginx**: 1.25+（高并发跳转）

## 核心特性

1. **短码生成**: 基于改良雪花算法 + 62进制转换，全局唯一
2. **高并发跳转**: Nginx直连Redis，绕过Java应用，响应时间<10ms
3. **缓存穿透防护**: 布隆过滤器 + 空值缓存双重防护
4. **恶意链接拦截**: 域名黑名单检测，支持人工审核
5. **过期管理**: Redis自动过期 + MySQL定时清理
6. **读写分离**: 生成链路与跳转链路彻底隔离

## 项目结构

```
backend/
├── pom.xml                          # Maven父工程配置
├── short-link-common/               # 公共模块
│   └── src/main/java/com/example/shortlink/common/
│       ├── id/SnowflakeIdGenerator.java    # 雪花算法生成器
│       └── util/ShortUrlUtil.java          # 62进制转换工具
└── short-link-service/              # 业务服务模块
    ├── pom.xml
    └── src/main/
        ├── java/com/example/shortlink/
        │   ├── ShortLinkApplication.java   # 启动类
        │   ├── config/                     # 配置类
        │   ├── controller/                 # 控制层
        │   ├── service/                    # 服务层
        │   ├── mapper/                     # 数据访问层
        │   ├── entity/                     # 实体类
        │   ├── dto/                        # 数据传输对象
        │   ├── enums/                      # 枚举类
        │   └── exception/                  # 异常处理
        └── resources/
            ├── application.yml             # 应用配置
            └── mapper/                     # MyBatis映射文件
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 数据库初始化

```sql
-- 执行初始化脚本
mysql -u root -p < sql/init.sql
```

### 配置修改

修改 `backend/short-link-service/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/example_db
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

shortlink:
  domain: https://your-short-domain.com
```

### 启动服务

```bash
cd backend
mvn clean package
java -jar short-link-service/target/short-link-service-1.0.0.jar
```

### Nginx配置

将 `nginx/conf/shortlink.conf` 配置到Nginx中，配置SSL证书后启动Nginx。

## API接口

### 1. 创建短链接

```
POST /api/shorten
Content-Type: application/json

{
  "longUrl": "https://example.com/very-long-url",
  "expireTime": "2025-12-31T23:59:59",
  "customCode": "mycode",
  "creator": "user123"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortUrl": "https://s.example.com/abc123",
    "shortCode": "abc123",
    "longUrl": "https://example.com/very-long-url",
    "expireTime": "2025-12-31T23:59:59",
    "createTime": "2024-01-01T12:00:00"
  }
}
```

### 2. 获取统计信息

```
GET /api/shorten/{shortCode}/stats
```

### 3. 禁用短链接

```
PUT /api/shorten/{shortCode}/disable
```

### 4. 启用短链接

```
PUT /api/shorten/{shortCode}/enable
```

### 5. 删除短链接

```
DELETE /api/shorten/{shortCode}
```

### 6. 兜底跳转

```
GET /api/shorten/{shortCode}/redirect
```

## 架构设计

### 跳转流程

```
用户请求 → Nginx → 布隆过滤器检查 → Redis查询 → 302重定向
                                      ↓
                                  Redis故障
                                      ↓
                              Java兜底接口 → MySQL查询 → 302重定向
```

### 生成流程

```
用户请求 → 参数校验 → 恶意域名检测 → 长链去重查询 → ID生成 → 62进制转换 → MySQL写入 → Redis缓存 → 返回短链接
```

## 安全特性

- **HTTPS**: 全站HTTPS，配置HSTS防劫持
- **Referer校验**: 防止跨域盗用
- **恶意域名拦截**: 自动检测钓鱼、病毒域名
- **内网地址拦截**: 禁止生成内网IP短链接

## 扩展功能

- 自定义短码
- 密码访问保护
- IP白名单限制
- PV/UV统计分析
- 地域/设备分析
- 批量生成短链接
