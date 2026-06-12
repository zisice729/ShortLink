
package com.example.shortlink.service.impl;

import com.example.shortlink.common.dto.request.ShortLinkCreateRequest;
import com.example.shortlink.common.dto.response.ShortLinkCreateResponse;
import com.example.shortlink.common.dto.response.ShortLinkStatsResponse;
import com.example.shortlink.common.entity.ShortLinkDO;
import com.example.shortlink.common.enums.StatusEnum;
import com.example.shortlink.common.exception.BusinessException;
import com.example.shortlink.common.id.SnowflakeIdGenerator;
import com.example.shortlink.common.util.ObjectUtil;
import com.example.shortlink.common.util.ShortUrlUtil;
import com.example.shortlink.mapper.ShortLinkMapper;
import com.example.shortlink.service.CacheService;
import com.example.shortlink.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

/**
 * 短链接服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl implements ShortLinkService {

    private final ShortLinkMapper shortLinkMapper;
    private final CacheService cacheService;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    @Value("${shortlink.domain}")
    private String shortLinkDomain;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortLinkCreateResponse createShortLink(ShortLinkCreateRequest request) {
        String longUrl = request.getLongUrl();
        String customCode = request.getCustomCode();
        
        validateLongUrl(longUrl);
        
        String longUrlMd5 = md5(longUrl);
        String existingShortCode = cacheService.getLongUrlMd5(longUrlMd5);
        
        if (ObjectUtil.isNotBlank(existingShortCode)) {
            ShortLinkDO existing = shortLinkMapper.selectByShortCode(existingShortCode);
            if (ObjectUtil.isNotNull(existing) && StatusEnum.NORMAL.getCode().equals(existing.getStatus())) {
                return buildResponse(existingShortCode, existing);
            }
        }

        String shortCode;
        if (ObjectUtil.isNotBlank(customCode)) {
            ShortLinkDO existing = shortLinkMapper.selectByShortCode(customCode);
            if (ObjectUtil.isNotNull(existing)) {
                throw new BusinessException("自定义短码已被占用");
            }
            shortCode = customCode;
        } else {
            long id = snowflakeIdGenerator.nextId();
            shortCode = ShortUrlUtil.numToFixedCode(id);
        }

        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .shortCode(shortCode)
                .longUrl(longUrl)
                .clickCount(0L)
                .expireTime(request.getExpireTime())
                .status(StatusEnum.NORMAL.getCode())
                .creator(request.getCreator())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        shortLinkMapper.insert(shortLinkDO);
        cacheService.setLongUrlMd5(longUrlMd5, shortCode);
        
        Long expireSeconds = null;
        if (ObjectUtil.isNotNull(request.getExpireTime())) {
            expireSeconds = java.time.Duration.between(LocalDateTime.now(), request.getExpireTime()).getSeconds();
            if (expireSeconds < 0) {
                expireSeconds = 0L;
            }
        }
        
        cacheService.setShortLink(shortCode, longUrl, expireSeconds);
        cacheService.addShortCode(shortCode);

        log.info("创建短链接成功: shortCode={}, longUrl={}", shortCode, longUrl);
        return buildResponse(shortCode, shortLinkDO);
    }

    @Override
    public String getLongUrl(String shortCode) {
        if (ObjectUtil.isBlank(shortCode)) {
            throw new BusinessException("短码不能为空");
        }

        if (cacheService.isNullCached(shortCode)) {
            throw new BusinessException("短链接不存在");
        }

        if (!cacheService.containsShortCode(shortCode)) {
            cacheService.setNullCache(shortCode);
            throw new BusinessException("短链接不存在");
        }

        String longUrl = cacheService.getShortLink(shortCode);
        if (ObjectUtil.isBlank(longUrl)) {
            ShortLinkDO shortLinkDO = shortLinkMapper.selectByShortCode(shortCode);
            if (ObjectUtil.isNull(shortLinkDO)) {
                cacheService.setNullCache(shortCode);
                throw new BusinessException("短链接不存在");
            }
            
            if (!StatusEnum.NORMAL.getCode().equals(shortLinkDO.getStatus())) {
                throw new BusinessException("短链接已禁用");
            }
            
            if (ObjectUtil.isNotNull(shortLinkDO.getExpireTime()) 
                    && shortLinkDO.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("短链接已过期");
            }
            
            longUrl = shortLinkDO.getLongUrl();
            Long expireSeconds = null;
            if (ObjectUtil.isNotNull(shortLinkDO.getExpireTime())) {
                expireSeconds = java.time.Duration.between(LocalDateTime.now(), shortLinkDO.getExpireTime()).getSeconds();
            }
            cacheService.setShortLink(shortCode, longUrl, expireSeconds);
        }
        
        return longUrl;
    }

    @Override
    public ShortLinkStatsResponse getStats(String shortCode) {
        ShortLinkDO shortLinkDO = shortLinkMapper.selectByShortCode(shortCode);
        if (ObjectUtil.isNull(shortLinkDO)) {
            throw new BusinessException("短链接不存在");
        }

        return ShortLinkStatsResponse.builder()
                .shortCode(shortLinkDO.getShortCode())
                .longUrl(shortLinkDO.getLongUrl())
                .clickCount(shortLinkDO.getClickCount())
                .status(shortLinkDO.getStatus())
                .expireTime(shortLinkDO.getExpireTime())
                .createTime(shortLinkDO.getCreateTime())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableShortLink(String shortCode) {
        ShortLinkDO shortLinkDO = shortLinkMapper.selectByShortCode(shortCode);
        if (ObjectUtil.isNull(shortLinkDO)) {
            throw new BusinessException("短链接不存在");
        }
        
        shortLinkDO.setStatus(StatusEnum.DISABLED.getCode());
        shortLinkDO.setUpdateTime(LocalDateTime.now());
        shortLinkMapper.updateById(shortLinkDO);
        
        cacheService.deleteShortLink(shortCode);
        log.info("禁用短链接: shortCode={}", shortCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableShortLink(String shortCode) {
        ShortLinkDO shortLinkDO = shortLinkMapper.selectByShortCode(shortCode);
        if (ObjectUtil.isNull(shortLinkDO)) {
            throw new BusinessException("短链接不存在");
        }
        
        shortLinkDO.setStatus(StatusEnum.NORMAL.getCode());
        shortLinkDO.setUpdateTime(LocalDateTime.now());
        shortLinkMapper.updateById(shortLinkDO);
        
        Long expireSeconds = null;
        if (ObjectUtil.isNotNull(shortLinkDO.getExpireTime())) {
            expireSeconds = java.time.Duration.between(LocalDateTime.now(), shortLinkDO.getExpireTime()).getSeconds();
        }
        cacheService.setShortLink(shortCode, shortLinkDO.getLongUrl(), expireSeconds);
        log.info("启用短链接: shortCode={}", shortCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShortLink(String shortCode) {
        ShortLinkDO shortLinkDO = shortLinkMapper.selectByShortCode(shortCode);
        if (ObjectUtil.isNull(shortLinkDO)) {
            throw new BusinessException("短链接不存在");
        }
        
        shortLinkDO.setStatus(StatusEnum.DELETED.getCode());
        shortLinkDO.setUpdateTime(LocalDateTime.now());
        shortLinkMapper.updateById(shortLinkDO);
        
        cacheService.deleteShortLink(shortCode);
        log.info("删除短链接: shortCode={}", shortCode);
    }

    @Override
    public void incrementClick(String shortCode) {
        cacheService.incrementClickCount(shortCode);
    }

    private void validateLongUrl(String longUrl) {
        if (ObjectUtil.isBlank(longUrl)) {
            throw new BusinessException("长链接不能为空");
        }
        
        if (longUrl.length() > 2048) {
            throw new BusinessException("长链接长度不能超过2048个字符");
        }

        if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
            throw new BusinessException("长链接必须以http://或https://开头");
        }

        if (longUrl.contains("localhost") || longUrl.contains("127.0.0.1") 
                || longUrl.contains("10.") || longUrl.contains("172.") || longUrl.contains("192.168.")) {
            throw new BusinessException("不支持内网地址");
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    private ShortLinkCreateResponse buildResponse(String shortCode, ShortLinkDO shortLinkDO) {
        return ShortLinkCreateResponse.builder()
                .shortUrl(shortLinkDomain + "/" + shortCode)
                .shortCode(shortCode)
                .expireTime(shortLinkDO.getExpireTime())
                .createTime(shortLinkDO.getCreateTime())
                .build();
    }
}
