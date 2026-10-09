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
     * 骑手确认取餐（status 仍为 3），记录取餐时间。
     * <p>P0-6：订单已生成取餐码时必须核销一致方可取餐（历史无码订单放行，避免存量数据被卡住）。</p>
     *
     * @param orderId    订单ID
     * @param riderId    骑手ID
     * @param pickupCode 骑手输入的取餐码
     */
    void pickupRiderTask(Long orderId, Long riderId, String pickupCode);

    /**
     * 骑手确认送达（3 → 4），校验归属后复用完成逻辑
     * @param orderId 订单ID
     * @param riderId 骑手ID
     */
    void deliverRiderOrder(Long orderId, Long riderId);

    /**
     * 店员核销自提订单：核对取餐码一致后完成订单（3 → 4）。
     * 仅自提单（source=SELF_PICKUP）可走此入口，防止自提单被不验码直接完成。
     *
     * @param orderId    订单ID
     * @param pickupCode 顾客出示的取餐码
     */
    void verifySelfPickupOrder(Long orderId, String pickupCode);

    /**
     * 订单改派：把在途订单（status 2 已指派 或 3 配送中）从原骑手转给目标骑手，
     * 同步调整双方在途单量，并记录改派轨迹。用于骑手转单与后台改派。
     *
     * @param orderId    订单ID
     * @param oldRiderId 原骑手ID（当前订单归属，须与订单一致）
     * @param newRiderId 目标骑手ID（须在线且未达在途上限）
     */
    void reassignRiderOrder(Long orderId, Long oldRiderId, Long newRiderId);
}