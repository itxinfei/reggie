package com.reggie.common.event;

/**
 * 订单完成事件
 * 在订单状态变为已完成时发布
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
public class OrderCompletedEvent extends OrderDomainEvent {

    public OrderCompletedEvent(Object source, Long orderId, Long tenantId) {
        super(source, orderId, tenantId);
    }
}
