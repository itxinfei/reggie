package com.reggie.module.order.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定「缺口4：库存回补幂等标记状态盲区」的修复。
 *
 * <p>审查曾断言库存回补置位条件三处不一致、置位失败即永不补偿。当前 HEAD 已抽离
 * {@code OrderStockRefundServiceImpl}，以 {@code orders.stock_refunded} 作为幂等标记，
 * 并在 {@code restoreForOrder} 首行以 {@code stockRefunded==1} 早退防重复回补；
 * 补偿任务 {@code StockRefundCompensationTask} 统一扫描 {@code in(已取消,已退款)} 且逐项 Redis 幂等。
 * 本测试不连库，断言早退守卫与"未回补才真正回补库存"的调用确实存在。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderStockRefundIdempotentTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderDetailService orderDetailService;

    @Mock
    private DishService dishService;

    @Spy
    @InjectMocks
    private OrderStockRefundServiceImpl service;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaUpdateChainWrapper stubChain() {
        // 默认 Answer：链式方法（eq/set/in/ne…）一律返回自身，update() 返回 true，
        // 规避 MyBatis-Plus LambdaUpdateChainWrapper 的 in 重载歧义与未桩方法 NPE。
        return org.mockito.Mockito.mock(LambdaUpdateChainWrapper.class, (org.mockito.stubbing.Answer) invocation -> {
            Class<?> rt = invocation.getMethod().getReturnType();
            if (boolean.class.equals(rt) || Boolean.class.equals(rt)) {
                return true;
            }
            // 链式方法（含泛型桥方法，返回类型可能为 Object）一律返回自身，保证 .eq().eq()… 连续调用不 NPE
            return invocation.getMock();
        });
    }

    private OrderDetail detail(Long dishId, int number) {
        OrderDetail d = new OrderDetail();
        d.setDishId(dishId);
        d.setNumber(number);
        return d;
    }

    @Test
    void restoreForOrder_alreadyRefunded_idempotentSkip() {
        Orders order = new Orders();
        order.setId(1L);
        order.setStatus(Orders.STATUS_CANCELLED);
        order.setStockRefunded(1); // 已回补
        when(orderService.getById(1L)).thenReturn(order);

        boolean r = service.restoreForOrder(1L);

        assertTrue(r);
        // 修复点：已标记 → 不再回补库存
        verify(dishService, never()).addStock(any(), any());
    }

    @Test
    void restoreForOrder_notRefunded_refundsStockOnceAndMarks() {
        Orders order = new Orders();
        order.setId(1L);
        order.setStatus(Orders.STATUS_CANCELLED);
        order.setStockRefunded(0);
        when(orderService.getById(1L)).thenReturn(order);
        when(orderDetailService.list(any())).thenReturn(Collections.singletonList(detail(10L, 2)));
        doReturn(stubChain()).when(orderService).lambdaUpdate();

        boolean r = service.restoreForOrder(1L);

        assertTrue(r);
        // 修复点：真正回补菜品库存（数量 2）
        verify(dishService).addStock(eq(10L), eq(new BigDecimal(2)));
        // 修复点：回补成功后置幂等标记
        verify(orderService).lambdaUpdate();
    }

    @Test
    void restoreForOrder_nullOrder_returnsFalse() {
        when(orderService.getById(1L)).thenReturn(null);

        assertFalse(service.restoreForOrder(1L));
        verify(dishService, never()).addStock(any(), any());
    }

    @Test
    void restoreForOrder_noDetails_marksWithoutStockCall() {
        Orders order = new Orders();
        order.setId(1L);
        order.setStatus(Orders.STATUS_REFUNDED);
        order.setStockRefunded(0);
        when(orderService.getById(1L)).thenReturn(order);
        when(orderDetailService.list(any())).thenReturn(Collections.emptyList());
        doReturn(stubChain()).when(orderService).lambdaUpdate();

        boolean r = service.restoreForOrder(1L);

        assertTrue(r);
        // 无明细：不回补库存但置标记，避免补偿任务反复扫描
        verify(dishService, never()).addStock(any(), any());
        verify(orderService).lambdaUpdate();
    }
}
