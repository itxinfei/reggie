package com.reggie.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.ai.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * AI用户画像 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /**
     * 根据用户ID查询画像
     *
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 用户画像
     */
    UserProfile selectByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
