package com.shortlink.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =====================================================
 * 短链生成响应DTO（Data Transfer Object）
 * =====================================================
 * 
 * 功能说明：
 *   返回给客户端的短链接生成结果
 * 
 * 响应字段：
 *   - shortUrl: 完整的短链接URL，可直接访问
 *   - shortCode: 短码部分
 *   - expireSecond: 过期时间（秒）
 * 
 * 使用场景：
 *   - POST /api/link/generate 接口的响应
 * 
 * 响应示例：
 * {
 *   "shortUrl": "https://s.xxx.com/abc123",
 *   "shortCode": "abc123",
 *   "expireSecond": 86400
 * }
 * 
 * @author 短链接系统团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRespDTO {

    /**
     * 完整短链接URL
     * 格式：https://s.xxx.com/{shortCode}
     * 可直接在浏览器中访问
     */
    private String shortUrl;

    /**
     * 短码
     * 格式：6-8位字母数字组合
     * 示例：abc123, xYZ789
     */
    private String shortCode;

    /**
     * 过期时间（秒）
     * 从创建时间开始计算的剩余有效时间
     * 
     * 示例：
     *   - 86400: 剩余1天
     *   - 604800: 剩余7天
     */
    private Long expireSecond;
}
