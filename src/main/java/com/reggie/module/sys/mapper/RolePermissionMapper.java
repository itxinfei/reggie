package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 角色权限关联 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
