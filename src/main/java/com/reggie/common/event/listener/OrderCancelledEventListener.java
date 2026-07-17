package com.reggie.common.event.listener;

import com.reggie.common.event.OrderCancelledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 订单取消事件监听器
 * 订单取消（超时/拒单/用户取消）后异步处理：
 * - 清理推荐缓存
 * - 记录通知消息
 * - 更新用户行为数据
 * </p>
 * <p>
 * 使用 @Async 异步执行，不影响订单取消主流程。
 * 监听器失败不会影响订单取消操作（事件已发布，只日志告警）。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
@Slf4j
@Component
public class OrderCancelledEventListener {

    /**
     * 处理订单取消事件
     * 异步执行，不阻塞订单取消主流程
     */
    @EventListener
    @Async("aiExecutor")
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Long orderId = event.getOrderId();
        Long tenantId = event.getTenantId();
        String reason = event.getReason();

        log.info("[事件] 订单取消事件触发: orderId={}, tenantId={}, reason={}", orderId, tenantId, reason);

        // TODO: 后续模块可在此处注册处理逻辑
        // 示例：
        // - 推荐模块：清除该订单关联的推荐缓存
        // - 通知模块：给用户发送取消通知
        // - 积分模块：如有积分变动需回退
    }
}
