package com.reggie.module.member.listener;

import com.reggie.common.BaseContext;
import com.reggie.common.event.OrderCompletedEvent;
import org.springframework.data.redis.core.RedisTemplate;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单完成事件监听器：成交后发放会员权益（积分 + 优惠券核销）
 * <p>覆盖 C 端外卖等不走收银台的直接成交链路，与收银台调用共享同一幂等服务。</p>
 *
 * 偏安全默认：失败不再仅 log.error，而是同时把订单 ID 写入 Redis 待对账集合
 * {@code member:reward:pending}，供后续对账/补偿任务拾起重放，避免权益发放失败被日志淹没。
 *
 * @author reggie
 * @since 2026-08-14
 */
@Slf4j
@Component
public class OrderCompletedListener {

    /** 待对账集合 key：失败发放的订单 ID 集合，补偿任务扫描并重放 */
    private static final String REWARD_PENDING_KEY = "member:reward:pending";

    /** 待对账集合 TTL（秒）：保留 24h，超过 24h 未重放视为人工介入 */
    private static final long REWARD_PENDING_TTL_SECONDS = 86400L;

    @Autowired
    private MemberRewardService memberRewardService;

    @Autowired
    private OrderService orderService;

    /**
     * Redis 为可选依赖：不可用时降级为仅打日志（不阻塞权益发放主流程）。
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Async("recommendExecutor")
    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        Long originalTenantId = BaseContext.getCurrentTenantId();
        BaseContext.setCurrentTenantId(event.getTenantId());
        try {
            Orders order = orderService.getById(event.getOrderId());
            if (order == null) {
                log.warn("[会员权益] 订单完成事件关联订单不存在: {}", event.getOrderId());
                return;
            }
            memberRewardService.grantReward(order);
            log.info("[会员权益] 订单{}完成后权益发放成功", event.getOrderId());
        } catch (Exception e) {
            Long orderId = event.getOrderId();
            log.error("[会员权益] 订单{}完成后权益发放失败，已加入待对账队列: {}", orderId, e.getMessage(), e);
            enqueuePendingReward(orderId, e);
        } finally {
            if (originalTenantId != null) {
                BaseContext.setCurrentTenantId(originalTenantId);
            } else {
                BaseContext.remove();
            }
        }
    }

    /**
     * 把失败订单 ID 写入 Redis 待对账集合，供补偿任务拾起重放。
     * Redis 不可用时不抛出异常，避免影响主流程。
     */
    private void enqueuePendingReward(Long orderId, Exception cause) {
        if (orderId == null || redisTemplate == null) {
            return;
        }
        try {
            // ZSET：score 用当前时间戳，便于按时间顺序拾取
            redisTemplate.opsForZSet().add(REWARD_PENDING_KEY,
                    String.valueOf(orderId), System.currentTimeMillis());
            redisTemplate.expire(REWARD_PENDING_KEY, REWARD_PENDING_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("[会员权益-对账] 订单{}已加入待对账队列 member:reward:pending", orderId);
        } catch (Exception e) {
            // 写入对账队列失败不影响业务，仅告警
            log.error("[会员权益-对账] 订单{}写入待对账队列失败: {}", orderId, e.getMessage(), e);
        }
    }
}
