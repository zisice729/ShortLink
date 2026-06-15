package com.shortlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =====================================================
 * 短链接系统启动类
 * =====================================================
 * 
 * 项目背景：
 *   短链接服务将长URL转换为短码，广泛应用于社交媒体分享、营销推广、短信营销等场景
 * 
 * 技术架构：
 *   - OpenResty(Nginx) + Lua: 高性能跳转，绕过Java直接访问Redis
 *   - SpringBoot: 短链生成（写链路）、降级兜底接口
 *   - Redis: 缓存层，包含短链映射、布隆过滤器、分布式锁
 *   - MySQL: 持久化存储
 *   - Kafka: 异步日志采集和统计
 * 
 * 核心流程：
 *   1. 写链路：用户提交长链接 → Java服务生成短码 → 写入MySQL+Redis+布隆过滤器
 *   2. 读链路：用户访问短链接 → OpenResty+Lua → Redis查询 → 302重定向
 *   3. 降级链路：Redis故障 → Nginx转发 → Java降级接口 → 查MySQL → 回写缓存
 * 
 * @author 短链接系统团队
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.shortlink.mapper")
public class ShortLinkApplication {

    /**
     * 应用启动入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ShortLinkApplication.class, args);
    }
}
