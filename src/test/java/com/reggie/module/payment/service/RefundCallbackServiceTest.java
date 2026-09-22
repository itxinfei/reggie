package com.reggie.module.payment.service;

import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderStockRefundService;
import com.reggie.module.payment.channel.notify.RefundNotifyResult;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.impl.RefundCallbackService;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RefundCallbackService} 单元测试：退款异步回调终态化决策与全额联动。
 * <p>
 * PaymentOrderService / OrderService 使用深度桩，避免 lambdaUpdate() 链式调用 NPE；
 * 断言聚焦可校验的决策与一次性副作用（库存/积分/售后）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public class RefundCallbackServiceTest {

    @Mock
    private PaymentOrderService paymentOrderService;

    @Mock
    private OrderService orderService;

    @Mock
    private RefundRecordMapper refundRecordMapper;

    @Mock
    private RefundRecordService refundRecordService;

    @Mock
    private OrderStockRefundService orderStockRefundService;

    @Mock
    private MemberRewardService memberRewardService;

    @InjectMocks
    private RefundCallbackService refundCallbackService;

    private static final String REFUND_NO = "RF20260921001";
    private static final Long PAYMENT_ORDER_ID = 7001L;
    private static final Long ORDER_ID = 5001L;
    private static final Long TENANT_ID = 10L;
    private static final BigDecimal AMOUNT = new BigDecimal("88.50");

    private RefundRecord staffRecord() {
        RefundRecord record = new RefundRecord();
        record.setId(1L);
        record.setRefundNo(REFUND_NO);
        record.setPaymentOrderId(PAYMENT_ORDER_ID);
        record.setTenantId(TENANT_ID);
        record.setAmount(AMOUNT);
        return record;
    }

    private PaymentOrder paymentOrder() {
        PaymentOrder po = new PaymentOrder();
        po.setId(PAYMENT_ORDER_ID);
        po.setOrderId(ORDER_ID);
        po.setTenantId(TENANT_ID);
        po.setAmount(AMOUNT);
        po.setStatus(PaymentOrder.STATUS_SUCCESS);
        return po;
    }

    private RefundNotifyResult notify(String status) {
        RefundNotifyResult result = new RefundNotifyResult();
        result.setSuccess(true);
        result.setOutRefundNo(REFUND_NO);
        result.setStatus(status);
        return result;
    }

    /** 显式 stub 支付单 lambdaUpdate 链（深桩对 MP 泛型 Children 解析不稳，故手工 mock）。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPaymentOrderChain() {
        LambdaUpdateChainWrapper chain = org.mockito.Mockito.mock(LambdaUpdateChainWrapper.class);
        when(paymentOrderService.lambdaUpdate()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.set(any(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(true);
    }

    /** 显式 stub 订单 lambdaUpdate 链。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubOrderChain() {
        LambdaUpdateChainWrapper chain = org.mockito.Mockito.mock(LambdaUpdateChainWrapper.class);
        when(orderService.lambdaUpdate()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.set(any(), any())).thenReturn(chain);
        when(chain.update()).thenReturn(true);
    }

    @Test
    public void handle_missingRecord_returnsFalse() {
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(null);

        boolean handled = refundCallbackService.handleRefundNotify(notify("SUCCESS"));

        assertFalse(handled, "本地缺记录应返回 false 触发渠道重试");
        verify(refundRecordService, never()).markProcessingToSuccess(any());
        verify(orderStockRefundService, never()).restoreForOrder(anyLong());
    }

    @Test
    public void handle_firstTimeFullRefund_invokesAllSideEffects() {
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(staffRecord());
        when(refundRecordService.markProcessingToSuccess(REFUND_NO)).thenReturn(true);
        when(paymentOrderService.getById(PAYMENT_ORDER_ID)).thenReturn(paymentOrder());
        // 累计已退 == 支付额：全额
        when(refundRecordService.sumRefundedAmount(PAYMENT_ORDER_ID)).thenReturn(AMOUNT);
        Orders order = new Orders();
        order.setId(ORDER_ID);
        order.setStatus(Orders.STATUS_ORDERED);
        when(orderService.getById(ORDER_ID)).thenReturn(order);
        stubPaymentOrderChain();
        stubOrderChain();

        boolean handled = refundCallbackService.handleRefundNotify(notify("SUCCESS"));

        assertTrue(handled);
        verify(orderStockRefundService).restoreForOrder(ORDER_ID);
        verify(memberRewardService).reverseRewards(ORDER_ID, TENANT_ID);
    }

    @Test
    public void handle_repeatedCallback_idempotentSkipsSideEffects() {
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(staffRecord());
        // 非首次：CAS PROCESSING→SUCCESS 失败（记录已是终态）
        when(refundRecordService.markProcessingToSuccess(REFUND_NO)).thenReturn(false);

        boolean handled = refundCallbackService.handleRefundNotify(notify("SUCCESS"));

        assertTrue(handled, "重复回调仍回成功 ACK");
        verify(paymentOrderService, never()).getById(anyLong());
        verify(orderStockRefundService, never()).restoreForOrder(anyLong());
        verify(memberRewardService, never()).reverseRewards(anyLong(), anyLong());
    }

    @Test
    public void handle_abnormalStatus_marksFailAndAlerts() {
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(staffRecord());

        boolean handled = refundCallbackService.handleRefundNotify(notify("ABNORMAL"));

        assertTrue(handled);
        verify(refundRecordService).markProcessingToFail(eq(REFUND_NO), eq("渠道终态: ABNORMAL"));
        verify(orderStockRefundService, never()).restoreForOrder(anyLong());
    }

    @Test
    public void handle_partialRefund_doesNotFlipStateOrInvokeSideEffects() {
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(staffRecord());
        when(refundRecordService.markProcessingToSuccess(REFUND_NO)).thenReturn(true);
        when(paymentOrderService.getById(PAYMENT_ORDER_ID)).thenReturn(paymentOrder());
        // 累计已退小于支付额：部分退款
        when(refundRecordService.sumRefundedAmount(PAYMENT_ORDER_ID)).thenReturn(new BigDecimal("30.00"));

        boolean handled = refundCallbackService.handleRefundNotify(notify("SUCCESS"));

        assertTrue(handled);
        verify(orderStockRefundService, never()).restoreForOrder(anyLong());
        verify(memberRewardService, never()).reverseRewards(anyLong(), anyLong());
    }

    @Test
    public void handle_afterSaleFirstTime_marksUserRefundSuccess() {
        RefundRecord afterSale = staffRecord();
        afterSale.setApplyUserId(9001L);
        when(refundRecordMapper.selectByRefundNoIgnoreTenant(REFUND_NO)).thenReturn(afterSale);
        when(refundRecordService.markProcessingToSuccess(REFUND_NO)).thenReturn(true);
        when(paymentOrderService.getById(PAYMENT_ORDER_ID)).thenReturn(paymentOrder());
        when(refundRecordService.sumRefundedAmount(PAYMENT_ORDER_ID)).thenReturn(AMOUNT);
        Orders order = new Orders();
        order.setId(ORDER_ID);
        // 售后 markUserRefundSuccess 已置 6，联动侧应幂等跳过订单更新
        order.setStatus(Orders.STATUS_REFUNDED);
        when(orderService.getById(ORDER_ID)).thenReturn(order);
        // 订单已退款，markOrderRefunded 会跳过订单链；仅需支付单链
        stubPaymentOrderChain();

        boolean handled = refundCallbackService.handleRefundNotify(notify("SUCCESS"));

        assertTrue(handled);
        verify(refundRecordService).markUserRefundSuccess(REFUND_NO);
        verify(orderStockRefundService).restoreForOrder(ORDER_ID);
    }
}
