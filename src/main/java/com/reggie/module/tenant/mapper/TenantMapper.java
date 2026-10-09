package com.reggie.module.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.tenant.model.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 租户 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}

