
package com.example.shortlink.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 短链接实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("short_url")
public class ShortLinkDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String shortCode;

    private String longUrl;

    private Long clickCount;

    private LocalDateTime expireTime;

    private Integer status;

    private String creator;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
