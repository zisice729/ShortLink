
package com.example.shortlink.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建短链接响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkCreateResponse {

    private String shortUrl;

    private String shortCode;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
