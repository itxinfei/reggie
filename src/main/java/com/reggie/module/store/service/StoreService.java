package com.reggie.module.store.service;

import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.store.dto.UpdateStoreDTO;
import com.reggie.module.store.model.StoreDailySummary;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.model.StoreSearchDTO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 门店管理服务接口
 * </p>
 * <p>提供总部-分店模式下的门店全生命周期管理</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface StoreService {

    /**
     * 创建门店（总部管理员操作）
     *
     * @param storeInfo 门店信息
     * @param tenant    租户基本信息
     * @param username  管理员账号
     * @param password  管理员密码
     * @return 创建的门店信息
     */
    StoreInfo createStore(StoreInfo storeInfo, Tenant tenant, String username, String password);

    /**
     * 更新门店信息（编辑）
     * 修改点：使用白名单 DTO 替代 Map，防止 mass assignment 攻击
     *
     * @param tenantId  门店tenantId
     * @param updateDTO 更新数据DTO（白名单字段）
     */
    void updateStore(Long tenantId, UpdateStoreDTO updateDTO);

    /**
     * 查询所有门店列表（总部视角，无分页，兼容旧接口）
     *
     * @return 门店列表（含经营概况）
     */
    List<Map<String, Object>> listAllStores();

    /**
     * 分页搜索门店列表（支持多条件筛选与排序）
     * 修改点：新增方法
     *
     * @param dto 搜索条件
     * @return 分页结果Map {records, total, pages, current, size}
     */
    Map<String, Object> searchStores(StoreSearchDTO dto);

    /**
     * 获取门店详情（含Tenant名称、今日经营数据）
     * 修改点：新增方法
     *
     * @param tenantId 门店ID
     * @return 门店详情Map
     */
    Map<String, Object> getStoreDetail(Long tenantId);

    /**
     * 查询分店列表（某总店下所有分店）
     *
     * @param parentTenantId 总店tenantId
     * @return 分店列表
     */
    List<StoreInfo> listBranchStores(Long parentTenantId);

    /**
     * 切换门店上下文（用于前端切换门店后数据隔离）
     *
     * @param targetTenantId 目标门店tenantId
     * @return 门店基本信息
     */
    Map<String, Object> switchStore(Long targetTenantId);

    /**
     * 获取门店今日经营概况
     *
     * @param tenantId 门店ID
     * @return 经营概况Map
     */
    Map<String, Object> getTodaySummary(Long tenantId);

    /**
     * 获取门店昨日经营汇总
     *
     * @param tenantId 门店ID
     * @return 昨日汇总数据
     */
    StoreDailySummary getYesterdaySummary(Long tenantId);

    /**
     * 更新门店状态（启用/停用）
     *
     * @param tenantId 门店ID
     * @param status   状态
     */
    void updateStoreStatus(Long tenantId, Integer status);

    /**
     * 批量更新门店状态
     * 修改点：新增方法
     *
     * @param tenantIds 门店ID列表
     * @param status    目标状态
     * @return 成功数量
     */
    int batchUpdateStoreStatus(List<Long> tenantIds, Integer status);

    /**
     * 导出门店数据
     * 修改点：新增方法
     *
     * @param keyword   关键词
     * @param storeType 门店类型
     * @param status    状态
     * @return 门店列表
     */
    List<Map<String, Object>> exportStores(String keyword, Integer storeType, Integer status);

    /**
     * 获取所有门店的经营数据汇总（总部控制台首页用）
     *
     * @return 汇总数据Map
     */
    Map<String, Object> getAggregatedDashboard();

    /**
     * 门店统计（总部视角，SQL 聚合）
     * <p>替代前端 listAllStores 拉全量后 filter 统计</p>
     *
     * @return 统计Map：totalStores/activeStores/inactiveStores/todayTotalStores
     */
    Map<String, Object> getStoreStats();

    /**
     * 根据租户 ID 查询门店信息（含营业时间等）
     * <p>域4 改造：从 RestaurantController 下沉</p>
     *
     * @param tenantId 租户 ID
     * @return 门店信息，不存在返回 null
     */
    StoreInfo findByTenantId(Long tenantId);

    // ==================== 集团汇总看板 ====================

    /**
     * 多店近 N 天营收趋势对比（按门店+日期分组）
     * <p>结果结构：{ dates: [...], stores: [ { tenantId, name, amounts: [...] }, ... ] }</p>
     *
     * @param days 天数，默认 7
     * @return 趋势数据
     */
    Map<String, Object> getMultiStoreTrend(int days);

    /**
     * 多品类销售对比（各店各品类的销售占比）
     * <p>结果结构：[ { tenantId, tenantName, categories: [ {categoryName, totalAmount, totalCount}, ... ] }, ... ]</p>
     *
     * @param startDate 起始日期（yyyy-MM-dd，可选，默认近 7 天）
     * @param endDate   结束日期（yyyy-MM-dd，可选，默认今天）
     * @return 品类对比数据
     */
    List<Map<String, Object>> getCategoryComparison(String startDate, String endDate);

    /**
     * 门店排行详情（近 N 天，营收/订单数/客单价三维）
     * <p>结果结构：[ { tenantId, storeName, totalAmount, orderCount, avgOrderAmount }, ... ]</p>
     *
     * @param days 天数，默认 7
     * @return 排行列表
     */
    List<Map<String, Object>> getRankingDetail(int days);
}

