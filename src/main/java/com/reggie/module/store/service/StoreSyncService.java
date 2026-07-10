package com.reggie.module.store.service;

import java.util.List;
import java.util.Map;

/**
 * 门店数据同步服务接口
 * 处理总部向分店同步菜品、分类、套餐、配置等数据
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface StoreSyncService {

    /**
     * 同步菜品到目标门店
     *
     * @param sourceTenantId 来源门店ID(总部)
     * @param targetTenantId 目标门店ID(分店)
     * @param dishIds        要同步的菜品ID列表（null或空=全量同步）
     * @param operatorId     操作人ID
     * @return 同步结果 {synced:int, failed:int, errors:[]}
     */
    Map<String, Object> syncDishes(Long sourceTenantId, Long targetTenantId,
                                    List<Long> dishIds, Long operatorId);

    /**
     * 同步分类到目标门店
     */
    Map<String, Object> syncCategories(Long sourceTenantId, Long targetTenantId, Long operatorId);

    /**
     * 同步套餐到目标门店
     */
    Map<String, Object> syncSetmeals(Long sourceTenantId, Long targetTenantId,
                                      List<Long> setmealIds, Long operatorId);

    /**
     * 同步优惠券模板到目标门店
     */
    Map<String, Object> syncCoupons(Long sourceTenantId, Long targetTenantId, Long operatorId);

    /**
     * 查询同步日志
     *
     * @param sourceTenantId 来源门店ID
     * @param page           页码
     * @param pageSize       每页大小
     * @return 分页同步日志
     */
    List<Map<String, Object>> getSyncLogs(Long sourceTenantId, int page, int pageSize);
}
