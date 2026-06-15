package com.shortlink.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * =====================================================
 * 短链接实体类（Data Object）
 * =====================================================
 * 
 * 功能说明：
 *   对应数据库short_link表，是短链接的持久化对象
 * 
 * 表字段映射：
 *   - id: 主键ID，自增
 *   - short_code: 短码，唯一索引
 *   - long_url: 原始长链接
 *   - url_md5: 标准化后链接的MD5值，用于去重
 *   - status: 状态（1正常 0禁用 2已过期）
 *   - expire_at: 过期时间
 *   - create_time: 创建时间
 *   - update_time: 更新时间
 * 
 * 使用场景：
 *   - MyBatis Plus CRUD操作
 *   - Service层与Mapper层之间的数据传输
 * 
 * @author 短链接系统团队
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("short_link")
public class ShortLinkDO {

    /**
     * 主键ID
     * 使用自增策略，由数据库自动生成
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 短码
     * 格式：6-8位字母数字组合
     * 示例：abc123, xYZ789
     * 唯一索引：uk_short_code
     */
    private String shortCode;

    /**
     * 原始长链接
     * 最大长度：1024字符
     * 示例：https://www.example.com/article/123456789
     */
    private String longUrl;

    /**
     * 标准化后链接的MD5值
     * 用于长链接去重，保证相同长链接返回相同短码
     * 长度：固定32位
     * 唯一索引：uk_url_md5
     */
    private String urlMd5;

    /**
     * 短链接状态
     * 取值：
     *   - 1: 正常，可访问
     *   - 0: 禁用，不可访问
     *   - 2: 已过期
     */
    private Integer status;

    /**
     * 过期时间
     * 为NULL表示永不过期
     * 为具体时间表示在该时间后不可访问
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     * 自动设置为当前时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 自动更新为当前时间
     */
    private LocalDateTime updateTime;
}
