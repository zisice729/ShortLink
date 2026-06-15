package com.shortlink.service.impl;

import com.shortlink.entity.ShortLinkDO;
import com.shortlink.mapper.ShortLinkMapper;
import com.shortlink.service.FallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * =====================================================
 * 降级服务实现类
 * =====================================================
 * 
 * 功能说明：
 *   实现Redis故障时的降级兜底逻辑
 * 
 * 降级场景：
 *   1. Redis连接失败
 *   2. Redis查询超时
 *   3. 布隆过滤器判断可能存在但Redis无数据
 * 
 * 降级流程：
 *   1. 从MySQL查询短链接信息
 *   2. 校验状态和过期时间
 *   3. 回写Redis缓存（修复一致性）
 *   4. 执行302重定向
 * 
 * @author 短链接系统团队
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FallbackServiceImpl implements FallbackService {

    /** 短链接Mapper */
    private final ShortLinkMapper shortLinkMapper;
    
    /** Redis模板 */
    private final RedisTemplate<String, Object> redisTemplate;

    /** 日期时间格式化器 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 根据短码查询短链接（从MySQL）
     * 
     * 查询策略：
     *   直接查询数据库，不经过Redis缓存
     *   用于Redis故障时的兜底查询
     * 
     * @param shortCode 短码
     * @return 短链接DO对象，未找到返回null
     */
    @Override
    public ShortLinkDO getByShortCode(String shortCode) {
        return shortLinkMapper.selectByShortCode(shortCode);
    }

    /**
     * 回写缓存（修复Redis）
     * 
     * 回写内容：
     *   - long_url: 原始长链接
     *   - status: 状态
     *   - expire_at: 过期时间
     * 
     * TTL设置：
     *   根据剩余过期时间动态计算
     *   如果已过期则设置为默认值（1天）
     * 
     * @param data 短链接DO对象
     */
    @Override
    public void refreshCache(ShortLinkDO data) {
        // 空数据直接返回
        if (data == null) {
            return;
        }

        // 构建Redis Key
        String shortKey = "short:" + data.getShortCode();
        
        // 计算剩余过期秒数
        long expireSecond = calculateExpireSecond(data.getExpireAt());

        // 回写Hash数据
        redisTemplate.opsForHash().put(shortKey, "long_url", data.getLongUrl());
        redisTemplate.opsForHash().put(shortKey, "status", data.getStatus());
        redisTemplate.opsForHash().put(shortKey, "expire_at", data.getExpireAt().format(FORMATTER));
        
        // 设置过期时间
        redisTemplate.expire(shortKey, expireSecond, TimeUnit.SECONDS);

        log.info("降级回写缓存成功: shortCode={}", data.getShortCode());
    }

    /**
     * 计算剩余过期秒数
     * 
     * 计算逻辑：
     *   expireAt - now = 剩余秒数
     *   如果已过期，返回默认值86400秒（1天）
     * 
     * @param expireAt 过期时间
     * @return 剩余过期秒数
     */
    private long calculateExpireSecond(LocalDateTime expireAt) {
        if (expireAt == null) {
            // 永不过期，返回默认值
            return 86400L;
        }
        
        // 计算剩余秒数
        return java.time.Duration.between(LocalDateTime.now(), expireAt).getSeconds();
    }
}
