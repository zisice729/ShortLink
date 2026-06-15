package com.shortlink.service.impl;

import com.shortlink.entity.ShortLinkDO;
import com.shortlink.entity.dto.GenerateReqDTO;
import com.shortlink.entity.dto.GenerateRespDTO;
import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.GenerateService;
import com.shortlink.util.Md5Util;
import com.shortlink.util.Num62Util;
import com.shortlink.util.UrlStandardUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================
 * 短链生成服务实现类
 * =====================================================
 * 
 * 功能说明：
 *   实现短链接生成的核心业务逻辑，包括长链接标准化、MD5去重、
 *   分布式锁控制、Redis自增ID生成、62进制短码转换等
 * 
 * 核心特性：
 *   1. 防重复生成：相同长链接返回相同短码
 *   2. 分布式锁：防止并发情况下的重复生成
 *   3. 双重检查：锁等待期间可能已创建，需要二次确认
 *   4. 布隆过滤器：快速判断短码是否存在
 * 
 * Redis Key设计：
 *   - long2short:{urlMd5} → shortCode（长链反向索引）
 *   - short:{shortCode} → Hash（短链映射缓存）
 *   - short_bloom → Set（布隆过滤器）
 *   - short_id_incr → String（自增ID）
 *   - lock:{urlMd5} → Lock（分布式锁）
 * 
 * @author 短链接系统团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateServiceImpl implements GenerateService {

    /** Redis模板，用于操作Redis */
    private final RedisTemplate<String, Object> redisTemplate;
    
    /** Redisson客户端，用于获取分布式锁 */
    private final RedissonClient redissonClient;
    
    /** 短链接Mapper，用于MySQL操作 */
    private final ShortLinkMapper shortLinkMapper;

    /** 长链反向索引Key前缀 */
    private static final String KEY_LONG_2_SHORT = "long2short:";
    
    /** Redis自增ID的Key */
    private static final String KEY_SHORT_ID_INCR = "short_id_incr";
    
    /** 分布式锁Key前缀 */
    private static final String KEY_LOCK_PREFIX = "lock:";
    
    /** 布隆过滤器Key */
    private static final String BLOOM_FILTER = "short_bloom";
    
    /** 长链反向索引缓存时间（天） */
    private static final int LONG_CACHE_TTL = 30;
    
    /** 短链接域名 */
    private static final String SHORT_LINK_DOMAIN = "https://s.xxx.com";

    /**
     * 生成短链接
     * 
     * 核心流程：
     *   1. 标准化长链接
     *   2. 计算MD5
     *   3. 查询Redis反向索引（第一次检查）
     *   4. 获取分布式锁
     *   5. 双重检查（第二次检查）
     *   6. Redis自增生成ID
     *   7. ID转62进制短码
     *   8. 写入MySQL
     *   9. 写入Redis Hash缓存
     *   10. 写入反向索引
     *   11. 添加到布隆过滤器
     *   12. 释放锁
     * 
     * @param request 短链生成请求参数
     * @return 短链生成响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateRespDTO generateShortUrl(GenerateReqDTO request) {
        // 获取原始长链接
        String rawUrl = request.getRawUrl();
        
        // 默认过期时间1天
        long expireSecond = request.getExpireSecond() != null ? request.getExpireSecond() : 86400L;

        // Step 1: 长链接标准化
        String standardUrl = UrlStandardUtil.standard(rawUrl);
        
        // Step 2: 计算MD5
        String urlMd5 = Md5Util.md5(standardUrl);

        // Step 3: 查询Redis反向索引（第一次检查）
        String existCode = (String) redisTemplate.opsForValue().get(KEY_LONG_2_SHORT + urlMd5);
        if (existCode != null) {
            log.info("长链接已存在短码: {}", existCode);
            return buildResponse(existCode, expireSecond);
        }

        // Step 4: 获取分布式锁
        String lockKey = KEY_LOCK_PREFIX + urlMd5;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 加锁，等待5秒，锁定5秒
            lock.lock(5, TimeUnit.SECONDS);

            // Step 5: 双重检查（锁等待期间可能已创建）
            existCode = (String) redisTemplate.opsForValue().get(KEY_LONG_2_SHORT + urlMd5);
            if (existCode != null) {
                log.info("双重检查发现已存在短码: {}", existCode);
                return buildResponse(existCode, expireSecond);
            }

            // Step 6: Redis自增ID
            Long id = redisTemplate.opsForValue().increment(KEY_SHORT_ID_INCR, 1);
            
            // Step 7: ID转62进制短码
            String shortCode = Num62Util.numTo62(id);

            // 计算过期时间
            LocalDateTime expireAt = LocalDateTime.now().plusSeconds(expireSecond);

            // 构建实体对象
            ShortLinkDO data = ShortLinkDO.builder()
                    .shortCode(shortCode)
                    .longUrl(rawUrl)
                    .urlMd5(urlMd5)
                    .status(1)
                    .expireAt(expireAt)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // Step 8: 写入MySQL
            shortLinkMapper.insert(data);

            // Step 9: 写入Redis Hash缓存
            String shortKey = "short:" + shortCode;
            redisTemplate.opsForHash().put(shortKey, "long_url", rawUrl);
            redisTemplate.opsForHash().put(shortKey, "status", 1);
            redisTemplate.opsForHash().put(shortKey, "expire_at", expireAt.toString());
            redisTemplate.expire(shortKey, expireSecond, TimeUnit.SECONDS);

            // Step 10: 写入反向索引（30天有效期）
            redisTemplate.opsForValue().set(KEY_LONG_2_SHORT + urlMd5, shortCode, LONG_CACHE_TTL, TimeUnit.DAYS);

            // Step 11: 添加到布隆过滤器
            redisTemplate.opsForSet().add(BLOOM_FILTER, shortCode);

            log.info("生成短链接成功: shortCode={}, longUrl={}", shortCode, rawUrl);
            return buildResponse(shortCode, expireSecond);

        } finally {
            // Step 12: 释放锁
            lock.unlock();
        }
    }

    /**
     * 构建响应对象
     * 
     * @param shortCode 短码
     * @param expireSecond 过期时间（秒）
     * @return 响应DTO
     */
    private GenerateRespDTO buildResponse(String shortCode, Long expireSecond) {
        return GenerateRespDTO.builder()
                .shortUrl(SHORT_LINK_DOMAIN + "/" + shortCode)
                .shortCode(shortCode)
                .expireSecond(expireSecond)
                .build();
    }
}
