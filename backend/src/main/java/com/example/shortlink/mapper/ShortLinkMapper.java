
package com.example.shortlink.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.shortlink.common.entity.ShortLinkDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 短链接Mapper
 */
@Mapper
public interface ShortLinkMapper extends BaseMapper<ShortLinkDO> {

    ShortLinkDO selectByShortCode(@Param("shortCode") String shortCode);

    ShortLinkDO selectByLongUrl(@Param("longUrl") String longUrl);

    List<ShortLinkDO> selectExpiredLinks(@Param("status") Integer status, @Param("expireTime") LocalDateTime expireTime);

    int updateClickCount(@Param("shortCode") String shortCode, @Param("count") Long count);
}
