package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.vo.DiningTablePublicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 桌台 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DiningTableMapper extends BaseMapper<DiningTable> {

    /**
     * 按区域(area_id)分组统计桌台数量（关联区域名称）
     * <p>用于区域统计页"最大容量区域"指标，替代前端 pageSize:999 拉全量后前端分组，避免全表扫描</p>
     *
     * @return 每个区域的 areaId/areaName/cnt
     */
    @Select("SELECT dt.area_id AS areaId, ta.name AS areaName, COUNT(*) AS cnt "
            + "FROM dining_table dt LEFT JOIN dining_area ta ON ta.id = dt.area_id "
            + "WHERE dt.tenant_id = #{tenantId} GROUP BY dt.area_id, ta.name")
    List<Map<String, Object>> statByArea(@Param("tenantId") Long tenantId);

    /**
     * 扫码点餐公开查询：按 id 返回桌台安全展示字段（名称/座位数/状态/区域），
     * 绕过租户拦截器（顾客扫码为匿名请求，无租户上下文）。
     * 仅返回非敏感展示信息，避免暴露 tenant_id/订单等内部数据；显式 is_deleted = 0 过滤已删除。
     *
     * @param id 桌台ID
     * @return 公开桌台视图，不存在返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT dt.id AS id, dt.name AS name, dt.seat_count AS seatCount, dt.status AS status, "
            + "ta.name AS areaName FROM dining_table dt LEFT JOIN dining_area ta ON ta.id = dt.area_id "
            + "WHERE dt.id = #{id} AND dt.is_deleted = 0")
    DiningTablePublicVO selectPublicById(@Param("id") Long id);

    /**
     * 扫码点餐公开菜单：按桌台 id 反查所属租户 id，绕过租户拦截器（匿名请求无租户上下文）。
     *
     * @param id 桌台ID
     * @return 桌台所属租户ID，桌台不存在返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT tenant_id FROM dining_table WHERE id = #{id} AND is_deleted = 0")
    Long selectTenantIdByIdIgnore(@Param("id") Long id);
}
