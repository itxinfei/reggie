package com.reggie.module.platform.adapter;

import com.reggie.module.platform.model.PlatformConfig;

import java.util.List;

/**
 * 外卖平台统一适配器接口
 * <p>
 * 各外卖平台（美团/饿了么/抖音等）实现此接口，隔离平台差异。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformAdapter {

    /**
     * 平台类型标识
     */
    String platformType();

    /**
     * 拉取增量订单（分页/时间窗）
     *
     * @param cfg     平台配置
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 标准化订单列表
     */
    List<PlatformOrder> pullOrders(PlatformConfig cfg, String beginTime, String endTime);

    /**
     * 状态回传：接单
     */
    void acceptOrder(PlatformConfig cfg, String platformOrderId);

    /**
     * 状态回传：拒单
     */
    void rejectOrder(PlatformConfig cfg, String platformOrderId);

    /**
     * 状态回传：出餐
     */
    void prepareOrder(PlatformConfig cfg, String platformOrderId);

    /**
     * 状态回传：完成
     */
    void completeOrder(PlatformConfig cfg, String platformOrderId);

    /**
     * 状态回传：取消
     */
    void cancelOrder(PlatformConfig cfg, String platformOrderId);

    /**
     * 商品同步：上架
     */
    void syncDishOnShelf(PlatformConfig cfg, Long dishId, String platformDishId);

    /**
     * 商品同步：下架
     */
    void syncDishOffShelf(PlatformConfig cfg, Long dishId, String platformDishId);

    /**
     * 库存同步：更新剩余可售数
     */
    void syncStock(PlatformConfig cfg, String platformDishId, int remainQty);

    /**
     * 营业状态同步：设置营业/休息
     */
    void syncBusinessStatus(PlatformConfig cfg, boolean open);

    /**
     * 连通性自检
     *
     * @return true=正常 false=异常
     */
    boolean healthCheck(PlatformConfig cfg);
}
