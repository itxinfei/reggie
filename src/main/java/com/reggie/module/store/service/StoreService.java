package com.reggie.module.store.service;

import com.reggie.entity.Tenant;
import com.reggie.module.store.model.StoreDailySummary;
import com.reggie.module.store.model.StoreInfo;

import java.util.List;
import java.util.Map;

/**
 * 门店管理服务
 * 提供总部-分店模式下的门店全生命周期管理
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
     * 查询所有门店列表（总部视角）
     *
     * @return 门店列表（含经营概况）
     */
    List<Map<String, Object>> listAllStores();

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
     * 获取所有门店的经营数据汇总（总部控制台首页用）
     *
     * @return 汇总数据Map
     */
    Map<String, Object> getAggregatedDashboard();
}
