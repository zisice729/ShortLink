
package com.example.shortlink.service.impl;

import com.example.shortlink.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    private static final String SHORT_LINK_KEY = "short:link:%s";
    private static final String LONG_URL_MD5_KEY = "short:md5:%s";
    private static final String NULL_CACHE_KEY = "short:null:%s";
    private static final String CLICK_COUNT_KEY = "short:click:%s";
    private static final String BLOOM_FILTER_NAME = "short:bloom";

    @Override
    public String getShortLink(String shortCode) {
        String key = String.format(SHORT_LINK_KEY, shortCode);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setShortLink(String shortCode, String longUrl, Long expireSeconds) {
        String key = String.format(SHORT_LINK_KEY, shortCode);
        redisTemplate.opsForValue().set(key, longUrl, expireSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void deleteShortLink(String shortCode) {
        String key = String.format(SHORT_LINK_KEY, shortCode);
        redisTemplate.delete(key);
    }

    @Override
    public String getLongUrlMd5(String longUrlMd5) {
        String key = String.format(LONG_URL_MD5_KEY, longUrlMd5);
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void setLongUrlMd5(String longUrlMd5, String shortCode) {
        String key = String.format(LONG_URL_MD5_KEY, longUrlMd5);
        redisTemplate.opsForValue().set(key, shortCode);
    }

    @Override
    public boolean containsShortCode(String shortCode) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        return bloomFilter.contains(shortCode);
    }

    @Override
    public void addShortCode(String shortCode) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        bloomFilter.add(shortCode);
    }

    @Override
    public void setNullCache(String shortCode) {
        String key = String.format(NULL_CACHE_KEY, shortCode);
        redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.SECONDS);
    }

    @Override
    public boolean isNullCached(String shortCode) {
        String key = String.format(NULL_CACHE_KEY, shortCode);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Long incrementClickCount(String shortCode) {
        String key = String.format(CLICK_COUNT_KEY, shortCode);
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long getClickCount(String shortCode) {
        String key = String.format(CLICK_COUNT_KEY, shortCode);
        String count = redisTemplate.opsForValue().get(key);
        return count != null ? Long.parseLong(count) : 0L;
    }
}
