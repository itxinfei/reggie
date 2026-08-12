package com.reggie.common.event;

import org.springframework.context.ApplicationEvent;

/**
 * 订单领域事件基类
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
public class OrderDomainEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final Long orderId;
    private final Long tenantId;

    public OrderDomainEvent(Object source, Long orderId, Long tenantId) {
        super(source);
        this.orderId = orderId;
        this.tenantId = tenantId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getTenantId() {
        return tenantId;
    }
}

