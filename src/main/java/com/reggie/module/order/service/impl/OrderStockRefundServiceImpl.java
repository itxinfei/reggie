package com.reggie.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.inventory.service.MaterialStockService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderStockRefundService;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.service.SetmealDishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单库存回补服务实现。
 *
 * <p>遍历逻辑（单品 / 套餐子菜品 × copies、菜品+原料联动、幂等标记）搬自
 * {@code OrderStatusFlowServiceImpl} 的成熟实现，保证回补口径一致；
 * 仅依赖 order/dish/setmeal/inventory，不依赖 payment，避免循环依赖。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Service
@Slf4j
public class OrderStockRefundServiceImpl implements OrderStockRefundService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealDishService setmealDishService;

    /** 原料库存联动（可选注入，按 BOM 恢复原料） */
    @Autowired(required = false)
    private MaterialStockService materialStockService;

    /** 库存操作函数式接口 */
    private interface StockOperation {
        boolean apply(Long dishId, BigDecimal qty);
    }

    @Override
    public boolean restoreForOrder(Long orderId) {
        if (orderId == null) {
            return true;
        }
        Orders order = orderService.getById(orderId);
        if (order == null) {
            log.warn("[库存回补] 订单不存在: orderId={}", orderId);
            return false;
        }
        // 幂等：已回补直接返回，防重复回补致库存膨胀
        if (order.getStockRefunded() != null && order.getStockRefunded() == 1) {
            return true;
        }
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);
        if (details == null || details.isEmpty()) {
            markStockRefunded(orderId);
            return true;
        }
        boolean allSuccess = true;
        for (OrderDetail detail : details) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);
            if (!processStockForItems(detail.getDishId(), detail.getSetmealId(), qty, this::refundStockAtomic)) {
                allSuccess = false;
            }
        }
        // 仅全部成功才标记；部分失败保留 stock_refunded=0，由补偿任务兜底重试
        if (allSuccess) {
            markStockRefunded(orderId);
        }
        return allSuccess;
    }

    /**
     * 处理菜品/套餐的库存回补：单品直接回补；套餐遍历 setmeal_dish 按 copies 展开。
     */
    private boolean processStockForItems(Long dishId, Long setmealId, BigDecimal quantity, StockOperation operation) {
        boolean success = true;

        if (dishId != null) {
            if (!operation.apply(dishId, quantity)) {
                success = false;
            }
        }

        if (setmealId != null) {
            LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
            sdWrapper.eq(SetmealDish::getSetmealId, setmealId);
            List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
            for (SetmealDish sd : setmealDishes) {
                int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                if (!operation.apply(sd.getDishId(), quantity.multiply(new BigDecimal(copies)))) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * 单项原子回补：菜品库存 + 起售状态恢复 + BOM 原料恢复。
     * 失败记录日志但不抛异常，避免单项失败影响其余项。
     */
    private boolean refundStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        try {
            dishService.addStock(dishId, qty);
            dishService.autoToggleSoldOut(dishId);
            if (materialStockService != null) {
                materialStockService.restoreMaterialStock(dishId, qty);
            }
            log.info("[库存回补] 菜品ID={} 回补{}份", dishId, qty);
            return true;
        } catch (Exception e) {
            log.error("[库存回补失败] 菜品ID={} 回补{}份失败: {}", dishId, qty, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 幂等置位 stock_refunded=1：匹配取消/退款终态、ne(stock_refunded,1) 防重复。
     */
    private void markStockRefunded(Long orderId) {
        orderService.lambdaUpdate()
                .eq(Orders::getId, orderId)
                .in(Orders::getStatus, Orders.STATUS_CANCELLED, Orders.STATUS_REFUNDED)
                .ne(Orders::getStockRefunded, 1)
                .set(Orders::getStockRefunded, 1)
                .update();
    }
}
