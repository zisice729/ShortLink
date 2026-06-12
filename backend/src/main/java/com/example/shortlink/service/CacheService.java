
package com.example.shortlink.service;

/**
 * 缓存服务接口
 */
public interface CacheService {

    String getShortLink(String shortCode);

    void setShortLink(String shortCode, String longUrl, Long expireSeconds);

    void deleteShortLink(String shortCode);

    String getLongUrlMd5(String longUrlMd5);

    void setLongUrlMd5(String longUrlMd5, String shortCode);

    boolean containsShortCode(String shortCode);

    void addShortCode(String shortCode);

    void setNullCache(String shortCode);

    boolean isNullCached(String shortCode);

    Long incrementClickCount(String shortCode);

    Long getClickCount(String shortCode);
}
