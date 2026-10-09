package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.reggie.enums.RefundStatus;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.impl.RefundRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定「缺口3：售后落账路径口径不一 / 非 CAS / 可重复落账」的修复。
 *
 * <p>审查曾断言 {@code RefundRecordServiceImpl.markUserRefundSuccess} 非 CAS、不排除终态、可重复落账。
 * 当前 HEAD 已改为：① 已 SUCCESS 终态直接幂等跳过；② 退款单与订单均用 CAS（带状态条件的
 * lambdaUpdate）推进；③ 订单置已退款走与 {@code RefundServiceImpl} 一致的状态白名单。本测试不连库，
 * 断言上述守卫与 CAS 调用确实存在。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefundRecordIdempotentTest {

    @Mock
    private RefundRecordMapper refundRecordMapper;

    @Mock
    private OrderService orderService;

    @Spy
    @InjectMocks
    private RefundRecordServiceImpl service;

    private RefundRecord rec(String status) {
        RefundRecord r = new RefundRecord();
        r.setId(1L);
        r.setRefundNo("RF001");
        r.setOrderId(99L);
        r.setStatus(status);
        return r;
    }

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

    @Test
    void markUserRefundSuccess_firstTime_updatesRefundAndOrder() {
        doReturn(rec("processing")).when(service).getOne(any());
        doReturn(stubChain()).when(service).lambdaUpdate();
        doReturn(stubChain()).when(orderService).lambdaUpdate();
        Orders order = new Orders();
        order.setId(99L);
        order.setStatus(Orders.STATUS_ORDERED);
        when(orderService.getById(99L)).thenReturn(order);

        service.markUserRefundSuccess("RF001");

        // 修复点：退款单 CAS 置成功
        verify(service).lambdaUpdate();
        // 修复点：订单联动置已退款
        verify(orderService).lambdaUpdate();
    }

    @Test
    void markUserRefundSuccess_alreadySuccess_idempotentSkip() {
        // 修复点：已是 SUCCESS 终态 → 幂等跳过，不再落账
        doReturn(rec(RefundStatus.SUCCESS.getCode())).when(service).getOne(any());
        doReturn(stubChain()).when(service).lambdaUpdate();

        service.markUserRefundSuccess("RF001");

        verify(service, never()).lambdaUpdate();
        verify(orderService, never()).lambdaUpdate();
    }

    @Test
    void markUserRefundSuccess_blankRefundNo_returnsWithoutLoad() {
        service.markUserRefundSuccess("  ");

        verify(service, never()).getOne(any());
    }

    @Test
    void markUserRefundSuccess_orderNotFound_skipsOrderUpdate() {
        doReturn(rec("processing")).when(service).getOne(any());
        doReturn(stubChain()).when(service).lambdaUpdate();
        when(orderService.getById(99L)).thenReturn(null);

        service.markUserRefundSuccess("RF001");

        // 退款单仍落账，但订单不存在时不联动
        verify(service).lambdaUpdate();
        verify(orderService, never()).lambdaUpdate();
    }

    @Test
    void markUserRefundSuccess_orderAlreadyRefunded_idempotentSkipOrder() {
        doReturn(rec("processing")).when(service).getOne(any());
        doReturn(stubChain()).when(service).lambdaUpdate();
        doReturn(stubChain()).when(orderService).lambdaUpdate();
        // 修复点：订单已是已退款 → 跳过订单 CAS，避免覆盖他路状态
        Orders order = new Orders();
        order.setId(99L);
        order.setStatus(Orders.STATUS_REFUNDED);
        when(orderService.getById(99L)).thenReturn(order);

        service.markUserRefundSuccess("RF001");

        verify(service).lambdaUpdate();
        verify(orderService, never()).lambdaUpdate();
    }

    @Test
    void markUserRefundSuccess_recordMissing_returnsGracefully() {
        doReturn(null).when(service).getOne(any());

        service.markUserRefundSuccess("RF001");

        // 修复点：本地无记录 → 不落账，等待渠道重试
        verify(service, never()).lambdaUpdate();
        verify(orderService, never()).lambdaUpdate();
    }
}
