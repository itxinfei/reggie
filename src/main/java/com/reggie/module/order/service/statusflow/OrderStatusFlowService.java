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

    /**
     * 店长派单：待接单(2) 且未指派 → 绑定骑手（status 仍为 2，待骑手接单）
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void dispatchOrder(Long orderId, Long riderId);

    /**
     * 骑手抢单：原子地绑定骑手并接单（2 → 3），以影响行数防并发双抢
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void grabOrder(Long orderId, Long riderId);

    /**
     * 骑手确认派单（2 → 3），校验订单确实指派给当前骑手
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void acceptRiderTask(Long orderId, Long riderId);

    /**
     * 骑手确认取餐（status 仍为 3），记录取餐时间
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void pickupRiderTask(Long orderId, Long riderId);

    /**
     * 骑手确认送达（3 → 4），校验归属后复用完成逻辑
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void deliverRiderOrder(Long orderId, Long riderId);
}