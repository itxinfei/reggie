package com.reggie.common.event.listener;

import com.reggie.common.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 订单完成事件监听器
 * 订单完成后异步处理：
 * - 更新推荐数据（用户喜好）
 * - 生成积分/成长值
 * - 触发评价提醒
 * </p>
 * <p>
 * 使用 @Async 异步执行，不影响订单完成主流程。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
@Slf4j
@Component
public class OrderCompletedEventListener {

    /**
     * 处理订单完成事件
     */
    @EventListener
    @Async("eventListenerExecutor")
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Long orderId = event.getOrderId();
        Long tenantId = event.getTenantId();

        log.info("[事件] 订单完成事件触发: orderId={}, tenantId={}", orderId, tenantId);

        // TODO: 后续模块可在此处注册处理逻辑
    }
}
