package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 派单超时回流任务。
 * <p>
 * 每 30 秒扫描各活跃租户：店长已指派骑手（rider_id 非空、status 仍为待接单 2）
 * 但超过阈值（默认 90 秒）骑手未接单的订单，清空 rider_id/dispatch_time 回流抢单大厅，
 * 避免订单长期挂在某个不在线或不应答的骑手名下变成死单。
 * </p>
 * <p>
 * 派单阶段不增加骑手负载（骑手接单时才 +1），故回流无需调整骑手计数。
 * 与 {@link UnacceptedOrderScanTask} 同模式：Redis 分布式锁 + 遍历租户设置 ThreadLocal。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Slf4j
@Component
public class RiderDispatchTimeoutTask {

    /** 扫描间隔（毫秒）：30 秒 */
    private static final long SCAN_INTERVAL_MS = 30 * 1000L;

    /** 分布式锁过期时间（秒） */
    private static final long LOCK_TTL_SECONDS = 20L;

    /** 派单后等待骑手接单的最长时间（秒），超时回流大厅 */
    @Value("${reggie.rider.dispatch-timeout-seconds:90}")
    private long dispatchTimeoutSeconds;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RedisLockUtil redisLockUtil;

    /**
     * 每 30 秒扫描超时未接的派单并回流大厅。
     */
    @Scheduled(fixedRate = SCAN_INTERVAL_MS)
    public void reflowTimeoutDispatches() {
        // 分布式锁防多实例重复回流（fail-closed：拿不到锁=另一实例在执行，跳过本轮）
        redisLockUtil.executeWithLock("schedule:lock:rider-dispatch-timeout", LOCK_TTL_SECONDS, () -> {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }
            LocalDateTime deadline = LocalDateTime.now().minusSeconds(dispatchTimeoutSeconds);
            int totalReflowed = 0;
            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
                    wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED)
                            .isNotNull(Orders::getRiderId)
                            .le(Orders::getDispatchTime, deadline)
                            .set(Orders::getRiderId, null)
                            .set(Orders::getDispatchTime, null);
                    int rows = orderService.getBaseMapper().update(null, wrapper);
                    if (rows > 0) {
                        log.info("[派单回流] 租户 {} 有 {} 个订单超时未接，已回流抢单大厅", tenant.getId(), rows);
                    }
                    totalReflowed += rows;
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个租户失败影响主流程
                    log.error("[派单回流] 租户 {} 回流失败: error={}", tenant.getId(), e.getMessage());
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalReflowed > 0) {
                log.info("[派单回流] 本轮扫描完成，共回流 {} 个超时订单", totalReflowed);
            }
        });
    }
}
