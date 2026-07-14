package com.reggie.module.store.service;

import com.reggie.entity.Tenant;
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
     * 修改点：新增方法，支持修改Tenant和StoreInfo
     *
     * @param tenantId  门店tenantId
     * @param updateData 更新数据Map
     */
    void updateStore(Long tenantId, Map<String, Object> updateData);

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
}
