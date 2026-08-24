package com.reggie.module.platform.service;

import com.reggie.module.platform.adapter.PlatformOrder;

import java.util.List;

/**
 * 平台订单落库服务
 * <p>
 * 将平台标准化订单转换为本地 orders / order_detail 记录，支持幂等去重。
 * 设计原则：复用订单域既有状态机与字段，平台订单在本地以独立标识字段区分来源，
 * 不直接调用 OrderService.submit（那是顾客端下单流程），避免引入购物车/用户校验等耦合。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformOrderPersistService {

    /**
     * 拉单后批量落库（幂等：已存在的平台订单号跳过）
     *
     * @param platformType     平台类型 MEITUAN / ELEME ...
     * @param platformShopId   平台侧门店ID
     * @param tenantId         本地租户ID（由调用方按当前上下文注入）
     * @param platformOrders   标准化订单列表
     * @return 实际新增的订单数
     */
    int persistOrders(String platformType, String platformShopId, Long tenantId,
                      List<PlatformOrder> platformOrders);

    /**
     * 判断指定平台订单号在当前租户下是否已存在（用于去重）
     *
     * @param platformType    平台类型
     * @param platformOrderId 平台订单号
     * @param tenantId        租户ID（去重需限定租户，避免跨门店误判）
     * @return true=已存在
     */
    boolean exists(String platformType, String platformOrderId, Long tenantId);
}
