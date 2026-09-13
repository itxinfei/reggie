package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.store.model.StoreInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 门店信息 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StoreInfoMapper extends BaseMapper<StoreInfo> {

    /**
     * 根据租户ID查询门店信息
     *
     * @param tenantId 租户ID
     * @return 门店信息
     */
    @Select("SELECT * FROM store_info WHERE tenant_id = #{tenantId}")
    StoreInfo findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查询某总店下所有分店
     *
     * @param parentTenantId 总店租户ID
     * @return 分店列表
     */
    @Select("SELECT * FROM store_info WHERE parent_tenant_id = #{parentTenantId}")
    List<StoreInfo> findByParentTenantId(@Param("parentTenantId") Long parentTenantId);

    /**
     * 根据门店编码查询
     * <p>打印代理注册（无登录会话）也走此方法，须绕过租户拦截器（store_code 全局唯一索引）。</p>
     *
     * @param storeCode 门店编码
     * @return 门店信息
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM store_info WHERE store_code = #{storeCode}")
    StoreInfo findByStoreCode(@Param("storeCode") String storeCode);

    /**
     * 分页搜索门店列表（支持多条件筛选与排序）
     * SQL定义在 resources/com/reggie/module/store/mapper/StoreInfoMapper.xml
     *
     * @param page 分页对象
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param tenantId 租户ID
     * @return 分页结果
     */
    IPage<Map<String, Object>> searchStores(Page<?> page, @Param("keyword") String keyword,
                                            @Param("storeType") Integer storeType,
                                            @Param("status") Integer status,
                                            @Param("sortBy") String sortBy,
                                            @Param("sortOrder") String sortOrder,
                                            @Param("tenantId") Long tenantId);

    /**
     * 统计门店数量（配合筛选条件）
     *
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param tenantId 租户ID
     * @return 总数
     */
    long countStores(@Param("keyword") String keyword,
                     @Param("storeType") Integer storeType,
                     @Param("status") Integer status,
                     @Param("tenantId") Long tenantId);

    /**
     * 查询门店明细（含Tenant名称、今日经营数据）
     *
     * @param tenantId 门店ID
     * @return 门店详情Map
     */
    Map<String, Object> searchStoreDetail(@Param("tenantId") Long tenantId);

    /**
     * 门店统计聚合（总部视角）
     * <p>单条 SQL 关联 tenant/store_daily_summary，替代前端 listAllStores 拉全量后 filter 统计，
     * 消除 N+1（原逐店查询 store_daily_summary）与全量内存计算</p>
     *
     * @param today 今日日期（用于关联当日日报汇总）
     * @return 聚合结果：totalStores/activeStores/inactiveStores/todayTotalStores
     */
    @Select("SELECT "
            + "COUNT(*) AS totalStores, "
            + "COALESCE(SUM(CASE WHEN t.status = 1 THEN 1 ELSE 0 END), 0) AS activeStores, "
            + "COALESCE(SUM(CASE WHEN t.status = 0 THEN 1 ELSE 0 END), 0) AS inactiveStores, "
            + "COALESCE(SUM(CASE WHEN sds.total_orders > 0 THEN 1 ELSE 0 END), 0) AS todayTotalStores "
            + "FROM store_info si "
            + "LEFT JOIN tenant t ON t.id = si.tenant_id "
            + "LEFT JOIN store_daily_summary sds ON sds.tenant_id = si.tenant_id AND sds.summary_date = #{today}")
    Map<String, Object> statStores(@Param("today") java.time.LocalDate today);

    /**
     * 导出全部门店数据（不限制数量，受筛选条件影响）
     *
     * @param keyword 关键词
     * @param storeType 门店类型
     * @param status 状态
     * @param tenantId 租户ID
     * @return 门店列表
     */
    List<Map<String, Object>> exportStores(@Param("keyword") String keyword,
                                           @Param("storeType") Integer storeType,
                                           @Param("status") Integer status,
                                           @Param("tenantId") Long tenantId);

    // ==================== 集团汇总看板：跨租户聚合查询 ====================

    /**
     * 多店近 N 天每日营收趋势（按门店+日期分组）
     * <p>绕过租户拦截器，查所有租户的完成订单汇总</p>
     *
     * @param startDate 起始时间（含）
     * @param endDate   结束时间（不含）
     * @param status    订单状态（应传已完成）
     * @return 每行：date, tenantId, totalAmount, orderCount
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT DATE(o.order_time) AS date, o.tenant_id AS tenantId, "
            + "SUM(o.amount) AS totalAmount, COUNT(*) AS orderCount "
            + "FROM orders o WHERE o.is_deleted = 0 AND o.status = #{status} "
            + "AND o.order_time >= #{startDate} AND o.order_time < #{endDate} "
            + "GROUP BY DATE(o.order_time), o.tenant_id ORDER BY date")
    List<Map<String, Object>> statDailyTrendByTenant(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status);

    /**
     * 多品类销售对比（按门店+品类分组）
     * <p>绕过租户拦截器，查所有租户的已完成订单明细</p>
     *
     * @param startDate 起始时间（含）
     * @param endDate   结束时间（不含）
     * @param status    订单状态（应传已完成）
     * @return 每行：tenantId, categoryName, totalAmount, totalCount
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT d.tenant_id AS tenantId, c.name AS categoryName, "
            + "SUM(od.amount) AS totalAmount, SUM(od.number) AS totalCount "
            + "FROM order_detail od "
            + "JOIN orders o ON od.order_id = o.id AND o.is_deleted = 0 AND o.status = #{status} "
            + "LEFT JOIN dish d ON od.dish_id = d.id AND d.is_deleted = 0 "
            + "LEFT JOIN category c ON d.category_id = c.id AND c.is_deleted = 0 "
            + "WHERE o.order_time >= #{startDate} AND o.order_time < #{endDate} "
            + "GROUP BY d.tenant_id, c.name ORDER BY totalAmount DESC")
    List<Map<String, Object>> statCategoryByTenant(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status);

    /**
     * 门店排行详情（近 N 天，营收/订单数/客单价三维）
     * <p>绕过租户拦截器，查所有租户的已完成订单汇总</p>
     *
     * @param startDate 起始时间（含）
     * @param endDate   结束时间（不含）
     * @param status    订单状态（应传已完成）
     * @return 每行：tenantId, orderCount, totalAmount, avgOrderAmount
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT o.tenant_id AS tenantId, COUNT(*) AS orderCount, "
            + "SUM(o.amount) AS totalAmount, "
            + "ROUND(SUM(o.amount)/COUNT(*), 2) AS avgOrderAmount "
            + "FROM orders o WHERE o.is_deleted = 0 AND o.status = #{status} "
            + "AND o.order_time >= #{startDate} AND o.order_time < #{endDate} "
            + "GROUP BY o.tenant_id ORDER BY totalAmount DESC")
    List<Map<String, Object>> statRankingDetail(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") Integer status);
}
