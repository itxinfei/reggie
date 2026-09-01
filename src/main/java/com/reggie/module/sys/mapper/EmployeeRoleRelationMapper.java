package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.model.EmployeeRoleRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 员工-角色关联 Mapper 接口
 * </p>
 * <p>查询统一使用 MP {@code LambdaQueryWrapper}，tenant_id 列由 TenantLineInnerInterceptor
 * 自动追加租户条件，无需手写 SQL。</p>
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@Mapper
public interface EmployeeRoleRelationMapper extends BaseMapper<EmployeeRoleRelation> {
}
