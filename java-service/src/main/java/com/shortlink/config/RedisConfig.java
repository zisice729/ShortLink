
package com.shortlink.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Redis配置类
 *
 * 功能说明：
 * - Redis基础配置类，用于初始化Redis连接相关参数
 * - 本项目主要使用Redisson作为Redis客户端（分布式锁、原子操作）
 * - Redis用途：
 *   1. 短链映射缓存：短码 -> 长链接 + 过期时间 + 状态
 *   2. 长链去重缓存：MD5(长链接) -> 短码，避免重复生成
 *   3. 布隆过滤器：存储所有有效短码，过滤无效请求
 *   4. 分布式锁：控制短链生成的并发冲突
 *
 * 高可用设计：
 * - Redis主从哨兵模式保证高可用
 * - 故障时自动降级到MySQL兜底查询
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 容器初始化完成后打印Redis配置就绪日志
     */
    @PostConstruct
    public void init() {
        log.info("Redis配置初始化完成");
    }
}
