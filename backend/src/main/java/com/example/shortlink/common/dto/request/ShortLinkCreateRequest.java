
package com.example.shortlink.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建短链接请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkCreateRequest {

    @NotBlank(message = "长链接不能为空")
    private String longUrl;

    private LocalDateTime expireTime;

    private String customCode;

    private String creator;
}
