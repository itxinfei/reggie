package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.inventory.service.MaterialStockService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.setmeal.service.SetmealDishService;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 库存回退补偿定时任务
 * </p>
 * <p>
 * 扫描近期取消/拒单的订单，自动重新回退未成功回退的菜品库存。
 * 每 30 分钟执行一次，仅处理 24 小时内的订单。
 * </p>
 *
 * @author reggie
 * @since 2026-07-15
 */
@Slf4j
@Component
public class StockRefundCompensationTask {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    /** 菜品服务 */
    @Autowired
    private DishService dishService;

    /** 原料库存服务（可选注入，补偿时按 BOM 同步恢复原料） */
    @Autowired(required = false)
    private MaterialStockService materialStockService;

    /** 租户服务（用于获取活跃租户列表） */
    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 公共 Redis 分布式锁工具（fail-closed） */
    @Autowired
    private RedisLockUtil redisLockUtil;

    /** 补偿任务检查时间窗口（24小时内） */
    private static final long COMPENSATION_WINDOW_HOURS = 24;

    /** 分布式锁过期时间（秒），应大于任务最大执行时间 */
    private static final long LOCK_TTL_SECONDS = 3 * 60L; // 3分钟

    /**
     * 库存回退补偿任务
     * 每 30 分钟执行一次，遍历所有活跃租户，扫描取消/拒单订单中库存未成功回退的记录
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void compensateStockRefund() {
        // 分布式锁防止任务重叠（fail-closed：拿不到锁=另一实例在执行，跳过本轮）
        redisLockUtil.executeWithLock("schedule:lock:stock-refund-compensation", LOCK_TTL_SECONDS, () -> {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }

            LocalDateTime since = LocalDateTime.now().minusHours(COMPENSATION_WINDOW_HOURS);
            int totalCompensated = 0;

            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    int count = compensateStockRefundForTenant(since);
                    totalCompensated += count;
                } finally {
                    BaseContext.remove();
                }
            }

            if (totalCompensated > 0) {
                log.info("[库存补偿] 批量补偿完成，共处理 {} 个租户，补偿 {} 个订单", tenants.size(), totalCompensated);
            }
        });
    }

    /**
     * 为单个租户执行库存回退补偿
     */
    private int compensateStockRefundForTenant(LocalDateTime since) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Orders::getStatus, Orders.STATUS_CANCELLED, Orders.STATUS_REFUNDED);
        wrapper.eq(Orders::getStockRefunded, 0);
        wrapper.ge(Orders::getUpdateTime, since);
        wrapper.eq(Orders::getTenantId, BaseContext.getCurrentTenantId());
        wrapper.orderByDesc(Orders::getUpdateTime);

        List<Orders> orders = orderService.list(wrapper);
        if (orders.isEmpty()) {
            return 0;
        }

        log.warn("[库存补偿] 租户 {} 发现 {} 个订单需要补偿回退库存",
            BaseContext.getCurrentTenantId(), orders.size());

        int compensated = 0;
        for (Orders order : orders) {
            try {
                compensateOrderStock(order.getId());
                compensated++;
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[库存补偿] 订单ID={} 补偿失败: {}", order.getId(), e.getMessage(), e);
            }
        }
        return compensated;
    }

    /**
     * 对单个订单执行库存回退补偿
     * 查询订单明细，逐项回退菜品/套餐库存
     */
    private void compensateOrderStock(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details == null || details.isEmpty()) {
            // 空明细订单无需回退库存，直接标记为已处理，避免重复扫描
            Orders order = new Orders();
            order.setId(orderId);
            order.setStockRefunded(1);
            orderService.updateById(order);
            log.info("[库存补偿] 订单ID={} 无明细，直接标记已处理", orderId);
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (OrderDetail detail : details) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);

            // 单品菜品：原子增加库存（Redis 幂等防重，避免部分失败重试时重复 addStock 致超原值 F2）
            if (detail.getDishId() != null) {
                String subKey = "dish:" + detail.getDishId();
                if (isCompensated(orderId, detail.getId(), subKey)) {
                    successCount++;
                } else if (refundStockAtomic(detail.getDishId(), qty)) {
                    markCompensated(orderId, detail.getId(), subKey);
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // 套餐：回退套餐内所有菜品的库存
            if (detail.getSetmealId() == null) {
                continue;
            }
            LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
            sdWrapper.eq(SetmealDish::getSetmealId, detail.getSetmealId());
            List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
            for (SetmealDish sd : setmealDishes) {
                int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                BigDecimal totalQty = qty.multiply(new BigDecimal(copies));
                // 套餐内菜品逐项幂等防重（Redis key 含 setmealDishId），部分失败重试时不重复 addStock（F2）
                String subKey = "sd:" + sd.getId();
                if (isCompensated(orderId, detail.getId(), subKey)) {
                    successCount++;
                } else if (refundStockAtomic(sd.getDishId(), totalQty)) {
                    markCompensated(orderId, detail.getId(), subKey);
                    successCount++;
                } else {
                    failCount++;
                }
            }
        }

        // 只有全部成功才标记为已处理；部分失败则保留待重试标记
        if (failCount == 0) {
            Orders order = new Orders();
            order.setId(orderId);
            order.setStockRefunded(1);
            orderService.updateById(order);
            log.info("[库存补偿] 订单ID={} 补偿成功，共{}项", orderId, successCount);
        } else {
            log.warn("[库存补偿] 订单ID={} 部分补偿失败，成功{}项，失败{}项，将重试",
                    orderId, successCount, failCount);
        }
    }

    /**
     * 原子增加菜品库存（补偿用）
     * 使用 LambdaUpdateWrapper 执行 SQL 原子更新
     *
     * @param dishId 菜品ID
     * @param qty    回退数量
     * @return 是否成功
     */
    private boolean refundStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        try {
            dishService.addStock(dishId, qty);

            // 原料库存联动：按 BOM 恢复原料（修复此前补偿只回菜品、原料永不回补）
            if (materialStockService != null) {
                materialStockService.restoreMaterialStock(dishId, qty);
            }

            // 回退后检查是否需要自动恢复起售
            try {
                dishService.autoToggleSoldOut(dishId);
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.debug("[库存补偿] 自动恢复起售检查失败: dishId={}", dishId);
            }
            log.info("[库存补偿] 菜品ID={} 回退{}份", dishId, qty);
            return true;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[库存补偿] 菜品ID={} 回退{}份失败: {}", dishId, qty, e.getMessage(), e);
            return false;
        }
    }

    // ──────────────────────────────────────
    // 库存补偿幂等辅助（Redis 防重，避免部分失败重试时重复 addStock 致库存超原值 F2）
    // ──────────────────────────────────────

    /** 补偿幂等 key TTL：略大于补偿窗口 24h，确保窗口内跨轮次有效 */
    private static final long COMPENSATE_KEY_TTL_HOURS = 25;

    private String compensateKey(Long orderId, Long detailId, String subKey) {
        return "stock:refund:" + orderId + ":" + detailId + ":" + subKey;
    }

    /** 该明细项是否已补偿过（Redis 不可用时返回 false，但此时 RedisLockUtil 未拿到锁已跳过整个任务，不会执行到此处） */
    private boolean isCompensated(Long orderId, Long detailId, String subKey) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(compensateKey(orderId, detailId, subKey)));
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.debug("[库存补偿] 幂等检查异常，按未补偿处理: {}", e.getMessage());
            return false;
        }
    }

    /** 标记该明细项已补偿成功（TTL 25h，覆盖 24h 补偿窗口） */
    private void markCompensated(Long orderId, Long detailId, String subKey) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(compensateKey(orderId, detailId, subKey), "1",
                    COMPENSATE_KEY_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.debug("[库存补偿] 幂等标记异常: {}", e.getMessage());
        }
    }
}







