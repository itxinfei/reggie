package com.reggie.module.schedule.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.Dish;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.entity.SetmealDish;
import com.reggie.entity.Tenant;
import com.reggie.service.DishService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /** 菜品服务（用于库存补偿） */

    /** 租户服务（用于获取活跃租户列表） */
    @Autowired
    private TenantService tenantService;

    /** 补偿任务检查时间窗口（24小时内） */
    private static final long COMPENSATION_WINDOW_HOURS = 24;

    /**
     * 库存回退补偿任务
     * 每 30 分钟执行一次，遍历所有活跃租户，扫描取消/拒单订单中库存未成功回退的记录
     */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void compensateStockRefund() {
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
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (OrderDetail detail : details) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);

            // 单品菜品：原子增加库存
            if (detail.getDishId() != null) {
                if (refundStockAtomic(detail.getDishId(), qty)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            // 套餐：回退套餐内所有菜品的库存
            if (detail.getSetmealId() != null) {
                LambdaQueryWrapper<SetmealDish> sdWrapper =
                        new LambdaQueryWrapper<>();
                sdWrapper.eq(SetmealDish::getSetmealId, detail.getSetmealId());
                List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
                for (com.reggie.entity.SetmealDish sd : setmealDishes) {
                    int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                    BigDecimal totalQty = qty.multiply(new BigDecimal(copies));
                    if (refundStockAtomic(sd.getDishId(), totalQty)) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                }
            }
        }

        // 标记库存已处理（无论全部成功还是部分失败）
        Orders order = new Orders();
        order.setId(orderId);
        order.setStockRefunded(1);
        orderService.updateById(order);

        if (failCount > 0) {
            log.warn("[库存补偿] 订单ID={} 补偿完成，成功{}项，失败{}项",
                    orderId, successCount, failCount);
        } else {
            log.info("[库存补偿] 订单ID={} 补偿成功，共{}项", orderId, successCount);
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

            // 回退后检查是否需要自动恢复起售
            try {
                dishService.autoToggleSoldOut(dishId);
            } catch (Exception e) {
                log.debug("[库存补偿] 自动恢复起售检查失败: dishId={}", dishId);
            }
            log.info("[库存补偿] 菜品ID={} 回退{}份", dishId, qty);
            return true;
        } catch (Exception e) {
            log.error("[库存补偿] 菜品ID={} 回退{}份失败: {}", dishId, qty, e.getMessage(), e);
            return false;
        }
    }
}
