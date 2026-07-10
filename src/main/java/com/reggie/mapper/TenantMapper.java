package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
