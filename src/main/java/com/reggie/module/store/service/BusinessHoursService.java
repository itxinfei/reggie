package com.reggie.module.store.service;

/**
 * 营业时间与暂停接单服务
 * 校验当前时间是否在营业时间内，管理暂停接单状态
 *
 * @author reggie
 * @since 2026-09-12
 */
public interface BusinessHoursService {

    /**
     * 校验当前时间是否在营业时间内且未暂停接单
     * <p>不在营业时间或已暂停则抛出 {@link com.reggie.common.CustomException}</p>
     *
     * @param tenantId 租户/门店 ID
     */
    void checkBusinessHours(Long tenantId);

    /**
     * 暂停/恢复接单
     *
     * @param tenantId 租户/门店 ID
     * @param pause    true=暂停接单，false=恢复接单
     */
    void togglePauseOrder(Long tenantId, boolean pause);
}
