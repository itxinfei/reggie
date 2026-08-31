package com.reggie.module.franchise.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.franchise.model.FranchiseSettlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 加盟分账结算单 Mapper
 *
 * @author reggie
 * @since 2026-08-15
 */
@Mapper
public interface FranchiseSettlementMapper extends BaseMapper<FranchiseSettlement> {

    /**
     * 结算单统计（总数、待确认、已确认、已结算）
     * <p>使用 SQL 聚合替代前端分页数据 filter 统计，避免跨页统计失真。
     * 需 @InterceptorIgnore 绕开租户拦截器：租户过滤由本 SQL 显式 #{tenantId} 控制。</p>
     *
     * @param tenantId 总部租户ID
     * @return 聚合结果：total/pending/confirmed/settled
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT "
            + "COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS pending, "
            + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS confirmed, "
            + "COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS settled "
            + "FROM franchise_settlement WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    Map<String, Object> statSettlements(@Param("tenantId") Long tenantId);
}
