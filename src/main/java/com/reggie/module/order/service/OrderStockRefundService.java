package com.reggie.module.order.service;

/**
 * 订单库存回补服务。
 *
 * <p>用于「退款」路径同步回补下单时扣减的库存；与
 * {@code OrderStatusFlowServiceImpl} 中取消/拒单的私有回补逻辑口径一致，
 * 但独立成服务，避免支付模块反向依赖状态机形成循环依赖。</p>
 */
public interface OrderStockRefundService {

    /**
     * 按订单回补库存（菜品 stock_qty + 按 BOM 回补原料），幂等。
     *
     * @param orderId 订单ID
     * @return 全部回补成功、或此前已回补返回 true；存在失败返回 false（交由补偿任务兜底）
     */
    boolean restoreForOrder(Long orderId);
}
