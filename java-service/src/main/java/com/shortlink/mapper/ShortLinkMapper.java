package com.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shortlink.entity.ShortLinkDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * =====================================================
 * 短链接Mapper接口
 * =====================================================
 * 
 * 功能说明：
 *   定义短链接数据库操作的接口，继承MyBatis Plus的BaseMapper
 * 
 * 提供方法：
 *   - selectByShortCode: 根据短码查询
 *   - selectByUrlMd5: 根据MD5查询
 *   - deleteExpiredOrDisabled: 批量删除过期或禁用的短链接
 * 
 * 使用说明：
 *   - Mapper接口由MyBatis Plus自动实现
 *   - XML实现放在 resources/mybatis/ 目录下
 * 
 * @author 短链接系统团队
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    /**
     * 根据短码查询短链接
     * 
     * 用途：
     *   - 查询指定短码的详细信息
     *   - 用于降级兜底接口查询MySQL
     * 
     * @param shortCode 短码
     * @return 短链接DO对象，未找到返回null
     */
    ShortLinkDO selectByShortCode(@Param("shortCode") String shortCode);

    /**
     * 根据MD5值查询短链接
     * 
     * 用途：
     *   - 查询指定长链接是否已生成短链
     *   - 用于去重校验
     * 
     * @param urlMd5 标准化后链接的MD5值
     * @return 短链接DO对象，未找到返回null
     */
    ShortLinkDO selectByUrlMd5(@Param("urlMd5") String urlMd5);

    /**
     * 批量删除过期或禁用的短链接
     * 
     * 用途：
     *   - 定时清理过期数据
     *   - 释放存储空间
     * 
     * 删除条件：
     *   - status != 1（禁用或已删除）
     *   - expire_at < now（已过期）
     * 
     * @param status 要删除的状态值
     * @param expireAt 过期时间阈值
     * @param limit 每批删除数量限制
     * @return 删除的记录数
     */
    int deleteExpiredOrDisabled(@Param("status") Integer status, @Param("expireAt") LocalDateTime expireAt, @Param("limit") Integer limit);
}
