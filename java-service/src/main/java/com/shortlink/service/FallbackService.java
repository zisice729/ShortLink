package com.shortlink.service;

import com.shortlink.entity.ShortLinkDO;

/**
 * =====================================================
 * 降级服务接口
 * =====================================================
 * 
 * 功能说明：
 *   定义Redis故障时的降级兜底逻辑
 * 
 * 使用场景：
 *   1. Redis连接失败
 *   2. Redis查询超时
 *   3. 缓存未命中
 * 
 * 核心逻辑：
 *   - 从MySQL查询短链接信息
 *   - 将查询结果回写到Redis缓存
 *   - 修复缓存，下次访问直接走Redis
 * 
 * @author 短链接系统团队
 */
public interface FallbackService {

    /**
     * 根据短码查询短链接（从MySQL）
     * 
     * 用途：
     *   Redis故障时，从数据库查询短链接信息
     * 
     * @param shortCode 短码
     * @return 短链接DO对象，未找到返回null
     */
    ShortLinkDO getByShortCode(String shortCode);

    /**
     * 回写缓存（修复Redis）
     * 
     * 用途：
     *   从MySQL查询到数据后，回写到Redis缓存
     *   修复缓存一致性，下次访问可以直接走Redis
     * 
     * @param data 短链接DO对象
     */
    void refreshCache(ShortLinkDO data);
}
