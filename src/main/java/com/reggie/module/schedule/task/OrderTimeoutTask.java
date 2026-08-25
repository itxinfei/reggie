package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.order.model.Orders;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.report.service.ReportService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 定时任务组件，包含订单超时自动取消、每日经营统计、库存预警等定时任务。
 * </p>
 * <p>
 * 注意：所有定时任务在调度线程中运行，无 HTTP 请求上下文。
 * 通过遍历活跃租户列表，为每个租户设置 ThreadLocal 上下文后执行业务逻辑。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 订单状态流转服务 */
    @Autowired
    private OrderStatusFlowService statusFlowService;

    /** 原料Mapper */
    @Autowired
    private MaterialMapper materialMapper;

    /** 报表服务 */
    @Autowired
    private ReportService reportService;

    /** 租户服务（用于获取活跃租户列表） */
    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 订单超时检查间隔（毫秒）：5分钟 */
    private static final long ORDER_TIMEOUT_CHECK_INTERVAL = 5 * 60 * 1000L;
    /** 订单超时阈值（分钟）：30分钟未接单自动取消 */
    private static final int ORDER_TIMEOUT_MINUTES = 30;
    /** 库存预警检查间隔（毫秒）：1小时 */
    private static final long INVENTORY_ALERT_CHECK_INTERVAL = 60 * 60 * 1000L;
    /** 分布式锁过期时间（毫秒），应大于任务最大执行时间 */
    private static final long LOCK_TTL_MS = 4 * 60 * 1000L; // 4分钟

    // ──────────────────────────────────────
    // 订单超时自动取消（每 5 分钟）
    // ──────────────────────────────────────
    @Scheduled(fixedRate = ORDER_TIMEOUT_CHECK_INTERVAL)
    public void cancelTimeoutOrders() {
        // 分布式锁防止任务重叠
        String lockValue = tryLock("schedule:lock:order-timeout", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[定时任务] 订单超时取消任务正在执行中，跳过本次");
            return;
        }

        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }

            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);
            int totalCancelled = 0;

            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    int count = cancelTimeoutOrdersForTenant(timeoutThreshold);
                    totalCancelled += count;
                } finally {
                    BaseContext.remove();
                }
            }

            if (totalCancelled > 0) {
                log.info("[定时任务] 订单超时取消完成，共处理 {} 个租户，取消 {} 个订单",
                    tenants.size(), totalCancelled);
            }
        } finally {
            unlock("schedule:lock:order-timeout", lockValue);
        }
    }

    /**
     * 为单个租户执行超时订单取消
     */
    private int cancelTimeoutOrdersForTenant(LocalDateTime timeoutThreshold) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 修复：覆盖两种超时场景 — PENDING_PAY（用户下单未付款）和 ORDERED（已付款/线下支付但未接单）。
        // 此前只查 ORDERED，新增的 PENDING_PAY 订单若用户长期不支付会变为孤儿单，需同样取消并回退库存。
        wrapper.in(Orders::getStatus, Orders.STATUS_PENDING_PAY, Orders.STATUS_ORDERED)
               .lt(Orders::getOrderTime, timeoutThreshold)
               .eq(Orders::getTenantId, BaseContext.getCurrentTenantId());

        List<Orders> timeoutOrders = orderService.list(wrapper);
        if (timeoutOrders.isEmpty()) {
            return 0;
        }

        log.info("[定时任务] 租户 {} 发现 {} 个超时未接单订单",
            BaseContext.getCurrentTenantId(), timeoutOrders.size());

        int cancelled = 0;
        for (Orders order : timeoutOrders) {
            try {
                statusFlowService.cancelOrder(order.getId(), "超时未接单，系统自动取消");
                log.warn("[定时任务] 订单超时自动取消: orderId={}, number={}, tenantId={}",
                    order.getId(), order.getNumber(), BaseContext.getCurrentTenantId());
                cancelled++;
            } catch (Exception e) {
                log.error("[定时任务] 取消超时订单失败: orderId={}, tenantId={}, error={}",
                    order.getId(), BaseContext.getCurrentTenantId(), e.getMessage());
            }
        }
        return cancelled;
    }

    // ──────────────────────────────────────
    // 每日经营统计（每天凌晨 2 点）
    // ──────────────────────────────────────
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyStatistics() {
        String lockValue = tryLock("schedule:lock:daily-statistics", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[定时任务] 每日经营统计任务正在执行中，跳过本次");
            return;
        }
        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }

            String yesterday = LocalDateTime.now().minusDays(1).toLocalDate().toString();

            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    Map<String, Object> report = reportService.getDailyReport(yesterday, tenant.getId());
                    log.info("[定时任务] 每日经营统计完成: date={}, tenantId={}, orders={}",
                        yesterday, tenant.getId(), report);
                } catch (Exception e) {
                    log.error("[定时任务] 每日统计失败: tenantId={}, error={}",
                        tenant.getId(), e.getMessage());
                } finally {
                    BaseContext.remove();
                }
            }
        } finally {
            unlock("schedule:lock:daily-statistics", lockValue);
        }
    }

    // ──────────────────────────────────────
    // 库存预警检查（每小时）
    // ──────────────────────────────────────
    @Scheduled(fixedRate = INVENTORY_ALERT_CHECK_INTERVAL)
    public void checkInventoryAlert() {
        String lockValue = tryLock("schedule:lock:inventory-alert", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[定时任务] 库存预警检查正在执行中，跳过本次");
            return;
        }
        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }

            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    checkInventoryAlertForTenant();
                } finally {
                    BaseContext.remove();
                }
            }
        } finally {
            unlock("schedule:lock:inventory-alert", lockValue);
        }
    }

    /**
     * 为单个租户检查库存预警
     * 注意：Material 表目前没有 tenant_id 列，预警为全局性检查
     */
    private void checkInventoryAlertForTenant() {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Material::getMinStock)
               .ne(Material::getMinStock, 0);

        List<Material> materials = materialMapper.selectList(wrapper);
        if (materials.isEmpty()) {
            return;
        }

        StringBuilder alertBuilder = new StringBuilder();
        int alertCount = 0;

        for (Material material : materials) {
            if (material.getStockQty() != null
                && material.getMinStock() != null
                && material.getStockQty().compareTo(material.getMinStock()) <= 0) {
                alertCount++;
                alertBuilder.append(String.format(
                    "[%s] 当前库存: %s, 预警线: %s; ",
                    material.getName(), material.getStockQty(), material.getMinStock()
                ));
            }
        }

        if (alertCount > 0) {
            log.warn("[定时任务] 库存预警(tenantId={}): 共{}个食材库存不足。{}",
                BaseContext.getCurrentTenantId(), alertCount, alertBuilder);
        }
    }

    // ──────────────────────────────────────
    // 分布式锁辅助方法
    // ──────────────────────────────────────

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁Key
     * @param ttlMs 锁过期时间（毫秒）
     * @return 锁值（UUID），获取失败返回null
     */
    private String tryLock(String lockKey, long ttlMs) {
        if (redisTemplate == null) {
            // fail-closed：写操作类定时任务在 Redis 不可用时跳过本次执行，避免多实例重复取消订单
            log.warn("[定时任务] Redis不可用，跳过本次执行（分布式锁获取失败）: {}", lockKey);
            return null;
        }
        try {
            // 使用UUID作为锁值，标识持有者身份
            String lockValue = java.util.UUID.randomUUID().toString();
            // SET NX EX：原子操作，不存在才设置并过期
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            // fail-closed：获取锁异常时跳过本次执行，避免多实例重复取消订单
            log.error("[定时任务] 获取分布式锁失败，跳过本次执行: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁（Lua脚本原子操作：比对锁值后才删除）
     * @param lockKey 锁Key
     * @param lockValue 锁值（UUID）
     */
    private void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            // Lua脚本：比对锁值后才删除，防止误删他人的锁
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                java.util.Collections.singletonList(lockKey),
                lockValue
            );
        } catch (Exception e) {
            log.error("[定时任务] 释放分布式锁失败: {}", lockKey, e);
        }
    }
}



