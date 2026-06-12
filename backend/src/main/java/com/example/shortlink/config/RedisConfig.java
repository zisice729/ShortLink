
package com.example.shortlink.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedissonClient redissonClient;

    @Value("${shortlink.bloom-filter.expected-insertions:10000000}")
    private long expectedInsertions;

    @Value("${shortlink.bloom-filter.false-positive-rate:0.001}")
    private double falsePositiveRate;

    @PostConstruct
    public void initBloomFilter() {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("short:bloom");
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(expectedInsertions, falsePositiveRate);
            log.info("布隆过滤器初始化完成: expectedInsertions={}, falsePositiveRate={}", 
                    expectedInsertions, falsePositiveRate);
        }
    }
}
