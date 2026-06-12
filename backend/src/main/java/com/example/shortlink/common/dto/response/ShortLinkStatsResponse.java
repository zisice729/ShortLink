
package com.example.shortlink.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短链接统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkStatsResponse {

    private String shortCode;

    private String longUrl;

    private Long clickCount;

    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
