package com.reggie.module.platform.service;

import com.reggie.module.platform.model.PlatformConfig;

import java.util.List;

/**
 * 外卖平台同步服务接口
 * <p>
 * 编排各平台适配器的调用，实现订单拉取、状态回传、商品同步等核心流程。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformSyncService {

    /**
     * 拉取指定平台的新订单
     *
     * @param config    平台配置
     * @param beginTime 开始时间（ISO 8601 格式）
     * @param endTime   结束时间（ISO 8601 格式）
     * @return 拉取的订单列表
     */
    List<com.reggie.module.platform.adapter.PlatformOrder> pullOrders(PlatformConfig config, String beginTime, String endTime);

    /**
     * 将拉取到的平台订单落库（幂等去重），tenant 取自当前上下文
     *
     * @param config    平台配置（含 platformType / shopId）
     * @param orders    平台订单列表
     * @return 实际新增订单数
     */
    int persistOrders(PlatformConfig config, List<com.reggie.module.platform.adapter.PlatformOrder> orders);

    /**
     * 回传订单状态到平台
     *
     * @param config          平台配置
     * @param platformOrderId 平台订单号
     * @param action          动作（accept/reject/prepare/complete/cancel）
     */
    void pushOrderStatus(PlatformConfig config, String platformOrderId, String action);

    /**
     * 同步商品状态到平台
     *
     * @param config          平台配置
     * @param dishId          本系统菜品 ID
     * @param platformDishId  平台菜品 ID
     * @param action          动作（on_shelf/off_shelf）
     */
    void syncDish(PlatformConfig config, Long dishId, String platformDishId, String action);

    /**
     * 同步库存到平台
     *
     * @param config          平台配置
     * @param platformDishId  平台菜品 ID
     * @param remainQty       剩余可售数
     */
    void syncStock(PlatformConfig config, String platformDishId, int remainQty);

    /**
     * 同步营业状态到平台
     *
     * @param config 平台配置
     * @param open   是否营业
     */
    void syncBusinessStatus(PlatformConfig config, boolean open);

    /**
     * 检查平台适配器是否健康
     *
     * @param config 平台配置
     * @return true=健康
     */
    boolean checkHealth(PlatformConfig config);
}
