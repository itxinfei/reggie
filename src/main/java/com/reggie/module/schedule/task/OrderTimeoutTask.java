package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.order.model.Orders;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.enums.OrderSource;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.RechargeRecordService;
import com.reggie.module.report.service.ReportService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /** 会员充值记录服务（充值单超时取消） */
    @Autowired
    private RechargeRecordService rechargeRecordService;

    /** 租户服务（用于获取活跃租户列表） */
    @Autowired
    private TenantService tenantService;

    /** 公共 Redis 分布式锁工具（fail-closed，见类 Javadoc 与 RedisLockUtil） */
    @Autowired
    private RedisLockUtil redisLockUtil;

    /** 订单超时检查间隔（毫秒）：5分钟 */
    private static final long ORDER_TIMEOUT_CHECK_INTERVAL = 5 * 60 * 1000L;
    /** 订单超时阈值（分钟）：30分钟未接单自动取消 */
    private static final int ORDER_TIMEOUT_MINUTES = 30;
    /** 配送超时自动完成阈值（小时）：24小时未确认收货自动完成 */
    private static final int DELIVERY_TIMEOUT_HOURS = 24;
    /** 配送超时检查间隔（毫秒）：10分钟 */
    private static final long DELIVERY_TIMEOUT_CHECK_INTERVAL = 10 * 60 * 1000L;
    /** 充值超时检查间隔（毫秒）：10分钟 */
    private static final long RECHARGE_TIMEOUT_CHECK_INTERVAL = 10 * 60 * 1000L;
    /** 充值超时阈值（分钟）：30分钟未支付自动取消 */
    private static final int RECHARGE_TIMEOUT_MINUTES = 30;
    /** 库存预警检查间隔（毫秒）：1小时 */
    private static final long INVENTORY_ALERT_CHECK_INTERVAL = 60 * 60 * 1000L;
    /** 分布式锁过期时间（秒），应大于任务最大执行时间 */
    private static final long LOCK_TTL_SECONDS = 4 * 60L; // 4分钟

    // ──────────────────────────────────────
    // 配送超时自动确认收货（每 10 分钟）
    // ──────────────────────────────────────
    /**
     * 处理 auto complete delivered orders。
     */
    @Scheduled(fixedRate = DELIVERY_TIMEOUT_CHECK_INTERVAL)
    public void autoCompleteDeliveredOrders() {
        redisLockUtil.executeWithLock("schedule:lock:delivery-timeout", LOCK_TTL_SECONDS, () -> {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }
            LocalDateTime threshold = LocalDateTime.now().minusHours(DELIVERY_TIMEOUT_HOURS);
            int totalCompleted = 0;
            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    totalCompleted += autoCompleteDeliveredOrdersForTenant(threshold);
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalCompleted > 0) {
                log.info("[定时任务] 配送超时自动完成收货完成，共处理 {} 个租户，完成 {} 个订单",
                    tenants.size(), totalCompleted);
            }
        });
    }

    /**
     * 为单个租户执行配送超时自动完成：STATUS_DELIVERING 且下单时间超过阈值则自动确认收货。
     * 兜底用户忘记点"确认收货"导致订单长期停在配送中的场景，便于结算与统计归档。
     */
    private int autoCompleteDeliveredOrdersForTenant(LocalDateTime threshold) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 修复(缺口5/FOCUS_REVIEW)：24h 自动确认收货只应作用于外卖配送单。
        // 堂食(EAT_IN)/排队(QUEUE)/预订(RESERVATION) 为到店消费，由门店员工完结并释放桌台，
        // 不应被"确认收货"，否则会错误完结堂食单、误触发订单完成事件与积分。
        // 兼容历史 source 为 null 的订单（一并纳入自动确认），仅排除明确的到店消费类型。
        wrapper.eq(Orders::getStatus, Orders.STATUS_DELIVERING)
               .and(w -> w.eq(Orders::getSource, OrderSource.TAKEOUT.getValue()).or().isNull(Orders::getSource))
               .lt(Orders::getOrderTime, threshold)
               .eq(Orders::getTenantId, BaseContext.getCurrentTenantId());
        List<Orders> deliveringOrders = orderService.list(wrapper);
        if (deliveringOrders.isEmpty()) {
            return 0;
        }
        log.info("[定时任务] 租户 {} 发现 {} 个超时未确认收货订单",
            BaseContext.getCurrentTenantId(), deliveringOrders.size());
        int completed = 0;
        for (Orders order : deliveringOrders) {
            try {
                statusFlowService.completeOrder(order.getId());
                log.warn("[定时任务] 配送超时自动完成收货: orderId={}, number={}, tenantId={}",
                    order.getId(), order.getNumber(), BaseContext.getCurrentTenantId());
                completed++;
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[定时任务] 自动完成订单失败: orderId={}, tenantId={}, error={}",
                    order.getId(), BaseContext.getCurrentTenantId(), e.getMessage());
            }
        }
        return completed;
    }

    // ──────────────────────────────────────
    // 订单超时自动取消（每 5 分钟）
    // ──────────────────────────────────────
    /**
     * 取消 timeout orders。
     */
    @Scheduled(fixedRate = ORDER_TIMEOUT_CHECK_INTERVAL)
    public void cancelTimeoutOrders() {
        // 分布式锁防止任务重叠（fail-closed：拿不到锁即另一实例在执行，跳过本轮）
        redisLockUtil.executeWithLock("schedule:lock:order-timeout", LOCK_TTL_SECONDS, () -> {
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
        });
    }

    /**
     * 为单个租户执行超时订单取消
     */
    private int cancelTimeoutOrdersForTenant(LocalDateTime timeoutThreshold) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        // 修复：覆盖两种超时场景 — PENDING_PAY（用户下单未付款）和 ORDERED（已付款/线下支付但未接单）。
        // 此前只查 ORDERED，新增的 PENDING_PAY 订单若用户长期不支付会变为孤儿单，需同样取消并回退库存。
        // 修复(2026-09-30)：到店消费单（堂食/排队/预订）落库为 PENDING_PAY 等待收银结账，无自动流转，
        // 不能套用"超时未接单"取消——否则堂食顾客用餐超 30 分钟订单被取消+库存回退，收银台永远无法结账。
        // 与 autoCompleteDeliveredOrdersForTenant 的排除口径一致；历史 source 为 null 的订单按外卖处理。
        wrapper.in(Orders::getStatus, Orders.STATUS_PENDING_PAY, Orders.STATUS_ORDERED)
               .lt(Orders::getOrderTime, timeoutThreshold)
               .eq(Orders::getTenantId, BaseContext.getCurrentTenantId())
               .and(w -> w.isNull(Orders::getSource)
                       .or().notIn(Orders::getSource,
                               OrderSource.EAT_IN.getValue(),
                               OrderSource.QUEUE.getValue(),
                               OrderSource.RESERVATION.getValue()));

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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[定时任务] 取消超时订单失败: orderId={}, tenantId={}, error={}",
                    order.getId(), BaseContext.getCurrentTenantId(), e.getMessage());
            }
        }
        return cancelled;
    }

    // ──────────────────────────────────────
    // 充值超时自动取消（每 10 分钟，P1-4）
    // ──────────────────────────────────────
    /**
     * 取消超时未支付的充值单。
     */
    @Scheduled(fixedRate = RECHARGE_TIMEOUT_CHECK_INTERVAL)
    public void cancelTimeoutRecharges() {
        redisLockUtil.executeWithLock("schedule:lock:recharge-timeout", LOCK_TTL_SECONDS, () -> {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(RECHARGE_TIMEOUT_MINUTES);
            int totalCancelled = 0;
            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    totalCancelled += cancelTimeoutRechargesForTenant(threshold);
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalCancelled > 0) {
                log.info("[定时任务] 充值超时取消完成，共处理 {} 个租户，取消 {} 个充值单",
                        tenants.size(), totalCancelled);
            }
        });
    }

    /**
     * 为单个租户取消超时充值单（CAS PENDING→CANCELLED，防支付回调并发）。
     */
    private int cancelTimeoutRechargesForTenant(LocalDateTime threshold) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeRecord::getStatus, RechargeRecord.STATUS_PENDING)
               .lt(RechargeRecord::getCreatedTime, threshold)
               .eq(RechargeRecord::getTenantId, BaseContext.getCurrentTenantId());
        List<RechargeRecord> timeoutList = rechargeRecordService.list(wrapper);
        if (timeoutList.isEmpty()) {
            return 0;
        }
        log.info("[定时任务] 租户 {} 发现 {} 个超时未支付充值单",
                BaseContext.getCurrentTenantId(), timeoutList.size());
        int cancelled = 0;
        for (RechargeRecord record : timeoutList) {
            try {
                boolean ok = rechargeRecordService.lambdaUpdate()
                        .eq(RechargeRecord::getId, record.getId())
                        .eq(RechargeRecord::getStatus, RechargeRecord.STATUS_PENDING)
                        .set(RechargeRecord::getStatus, RechargeRecord.STATUS_CANCELLED)
                        .update();
                if (ok) {
                    cancelled++;
                    log.warn("[定时任务] 充值单超时自动取消: rechargeId={}, rechargeNo={}, tenantId={}",
                            record.getId(), record.getRechargeNo(), BaseContext.getCurrentTenantId());
                }
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[定时任务] 取消超时充值单失败: rechargeId={}, error={}",
                        record.getId(), e.getMessage());
            }
        }
        return cancelled;
    }

    // ──────────────────────────────────────
    // 每日经营统计（每天凌晨 2 点）
    // ──────────────────────────────────────
    /**
     * 处理 daily statistics。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyStatistics() {
        redisLockUtil.executeWithLock("schedule:lock:daily-statistics", LOCK_TTL_SECONDS, () -> {
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
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.error("[定时任务] 每日统计失败: tenantId={}, error={}",
                        tenant.getId(), e.getMessage());
                } finally {
                    BaseContext.remove();
                }
            }
        });
    }

    // ──────────────────────────────────────
    // 库存预警检查（每小时）
    // ──────────────────────────────────────
    /**
     * 校验 inventory alert。
     */
    @Scheduled(fixedRate = INVENTORY_ALERT_CHECK_INTERVAL)
    public void checkInventoryAlert() {
        redisLockUtil.executeWithLock("schedule:lock:inventory-alert", LOCK_TTL_SECONDS, () -> {
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
        });
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
}



