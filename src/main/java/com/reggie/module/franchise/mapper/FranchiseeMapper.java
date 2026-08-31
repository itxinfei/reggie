package com.reggie.module.franchise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.franchise.model.Franchisee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 加盟商 Mapper
 *
 * @author reggie
 * @since 2026-08-15
 */
@Mapper
public interface FranchiseeMapper extends BaseMapper<Franchisee> {

    /**
     * 加盟商统计（总数、启用、禁用、关联合同数）
     * <p>使用 SQL 聚合替代前端分页数据 filter 统计，避免跨页统计失真。
     * 需 @InterceptorIgnore 绕开租户拦截器：租户过滤由本 SQL 显式 #{tenantId} 控制。</p>
     *
     * @param tenantId 总部租户ID
     * @return 聚合结果：total/enabled/disabled/contractCount
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT "
            + "COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS enabled, "
            + "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS disabled, "
            + "COALESCE((SELECT COUNT(DISTINCT franchisee_id) FROM franchise_contract "
            + "WHERE tenant_id = #{tenantId} AND is_deleted = 0), 0) AS contractCount "
            + "FROM franchisee WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Map<String, Object> statFranchisees(@Param("tenantId") Long tenantId);
}
