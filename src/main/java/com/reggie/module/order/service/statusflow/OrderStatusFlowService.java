package com.reggie.module.order.service.statusflow;

/**
 * 订单状态流转服务
 * 封装订单在后台视角下的状态变更：接单、拒单、完成、取消，
 * 以及状态名称解析和按订单ID回退库存等配套操作。
 *
 * 从 {@link com.reggie.module.order.service.OrderService} 中拆分，
 * 使父接口保持下单/查询/幂等性的单一职责。
 *
 * @author reggie
 * @since 2026-08-22
 */
public interface OrderStatusFlowService {

    /**
     * 按目标状态更新订单（状态机总入口）
     * @param status 目标状态码
     * @param id     订单ID
     */
    void updateStatus(Integer status, Long id);

    /**
     * 接单：待接单(2) → 配送中(3)
     * @param id 订单ID
     */
    void confirmOrder(Long id);

    /**
     * 拒单：待接单(2) → 已取消(5)，同时回退库存与会员权益
     * @param id 订单ID
     */
    void rejectOrder(Long id);

    /**
     * 完成订单：配送中(3) → 已完成(4)
     * @param id 订单ID
     */
    void completeOrder(Long id);

    /**
     * 取消订单：任意非完成/取消状态 → 已取消(5)，同时回退库存与会员权益
     * @param id     订单ID
     * @param reason 取消原因（可选）
     */
    void cancelOrder(Long id, String reason);
}