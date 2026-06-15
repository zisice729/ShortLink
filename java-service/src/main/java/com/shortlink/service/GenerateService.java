package com.shortlink.service;

import com.shortlink.entity.dto.GenerateReqDTO;
import com.shortlink.entity.dto.GenerateRespDTO;

/**
 * =====================================================
 * 短链生成服务接口
 * =====================================================
 * 
 * 功能说明：
 *   定义短链接生成的核心业务逻辑
 * 
 * 核心流程：
 *   1. 长链接标准化（去重的前提）
 *   2. MD5计算（生成唯一标识）
 *   3. 防重复校验（Redis反向索引）
 *   4. 分布式锁（防止并发重复）
 *   5. ID生成（Redis自增）
 *   6. 短码转换（62进制）
 *   7. 数据持久化（MySQL）
 *   8. 缓存构建（Redis + 布隆过滤器）
 * 
 * @author 短链接系统团队
 */
public interface GenerateService {

    /**
     * 生成短链接
     * 
     * 处理流程：
     *   1. 校验长链接格式
     *   2. 标准化长链接
     *   3. 计算MD5
     *   4. 检查是否已存在（Redis去重）
     *   5. 获取分布式锁（防止并发重复）
     *   6. 双重检查（锁等待期间可能已创建）
     *   7. 生成自增ID
     *   8. 转换为62进制短码
     *   9. 写入MySQL
     *   10. 写入Redis缓存
     *   11. 添加到布隆过滤器
     *   12. 释放锁
     * 
     * @param request 短链生成请求参数
     * @return 短链生成响应，包含完整短链接URL
     */
    GenerateRespDTO generateShortUrl(GenerateReqDTO request);
}
