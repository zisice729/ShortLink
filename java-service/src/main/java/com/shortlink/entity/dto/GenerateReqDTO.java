package com.shortlink.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * =====================================================
 * 短链生成请求DTO（Data Transfer Object）
 * =====================================================
 * 
 * 功能说明：
 *   接收客户端创建短链接的请求参数
 * 
 * 校验规则：
 *   - rawUrl: 不能为空，必须是有效的URL格式
 *   - expireSecond: 可选，默认86400秒（1天）
 * 
 * 使用场景：
 *   - POST /api/link/generate 接口的请求参数
 * 
 * @author 短链接系统团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReqDTO {

    /**
     * 原始长链接
     * 不能为空，必须是有效的HTTP/HTTPS URL
     * 最大长度：1024字符
     */
    @NotBlank(message = "长链接不能为空")
    private String rawUrl;

    /**
     * 过期时间（秒）
     * 可选字段，不传则使用默认值
     * 默认值：86400秒（1天）
     * 最大值：建议不超过31536000秒（1年）
     * 
     * 示例：
     *   - 86400: 1天
     *   - 604800: 7天
     *   - 2592000: 30天
     */
    private Long expireSecond;
}
