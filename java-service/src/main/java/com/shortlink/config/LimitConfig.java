
package com.shortlink.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置类
 *
 * 功能说明：
 * - 从application.yml读取限流相关配置参数
 * - 支持QPS限流、突发容量、滑动窗口配置
 * - 初始化时打印配置参数日志，便于运维监控
 *
 * 配置项说明：
 * - maxQps：最大每秒请求数，默认10000
 * - burstCapacity：令牌桶容量，默认1000
 * - windowSeconds：滑动窗口时间，默认1秒
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "shortlink.limit")
public class LimitConfig {

    /** 最大QPS，每秒允许通过的最大请求数 */
    private int maxQps = 10000;

    /** 令牌桶容量，支持突发流量的最大令牌数 */
    private int burstCapacity = 1000;

    /** 滑动窗口时间窗口大小（秒）*/
    private int windowSeconds = 1;

    /**
     * 容器初始化完成后打印限流配置信息
     * 用于确认配置是否正确加载
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("限流配置初始化完成: maxQps={}, burstCapacity={}, windowSeconds={}",
                maxQps, burstCapacity, windowSeconds);
    }
}
