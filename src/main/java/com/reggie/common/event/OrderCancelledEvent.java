package com.reggie.common.event;

/**
 * 订单取消事件
 * 在订单取消（超时/拒单/用户取消）时发布
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
public class OrderCancelledEvent extends OrderDomainEvent {

    private final String reason;

    public OrderCancelledEvent(Object source, Long orderId, Long tenantId, String reason) {
        super(source, orderId, tenantId);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
