package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.CustomException;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.channel.RefundRequest;
import com.reggie.module.payment.channel.RefundResponse;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import com.reggie.module.payment.service.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import static com.reggie.module.payment.model.PaymentOrder.STATUS_REFUND;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;

/**
 * 退款服务实现：按业务订单全额退款。
 * <p>
 * 逻辑与 {@code PaymentController.refund} 的"渠道调用 + 本地记账 + 订单联动"一致，
 * 但按 orderId 定位支付单，专供订单取消/拒单自动退款使用。
 * 三段式设计：
 * 1. 查询与累计校验（事务外，避免长事务持锁）
 * 2. 调用支付渠道退款（事务外，外部 HTTP 不应被事务包裹）
 * 3. 事务内本地落库（行锁防并发、CAS 防覆盖、全额时联动订单为已退款6）
 * </p>
 *
 * @author reggie
 * @since 2026-08-30
 */
@Slf4j
@Service
public class RefundServiceImpl implements RefundService {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @Autowired
    private MemberRewardService memberRewardService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Override
    public boolean refundByOrder(Long orderId, String reason) {
        if (orderId == null) {
            return false;
        }
        // === 1. 查询该订单最新 SUCCESS 支付单（未支付订单无 SUCCESS 支付单，无需退款） ===
        PaymentOrder paymentOrder = paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getOrderId, orderId)
                .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                .orderByDesc(PaymentOrder::getId)
                .last("limit 1")
                .one();
        if (paymentOrder == null) {
            log.info("订单自动退款跳过：未发现已成功支付的支付单 orderId={}", orderId);
            return false;
        }
        BigDecimal paymentAmount = paymentOrder.getAmount();
        if (paymentAmount == null) {
            log.warn("订单自动退款跳过：支付单金额缺失，数据异常 orderId={}, paymentOrderId={}",
                    orderId, paymentOrder.getId());
            return false;
        }
        BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrder.getId());
        if (alreadyRefunded.compareTo(paymentAmount) >= 0) {
            log.info("订单自动退款幂等跳过：已全额退款 orderId={}, paymentOrderId={}", orderId, paymentOrder.getId());
            return false;
        }
        BigDecimal refundAmount = paymentAmount.subtract(alreadyRefunded);

        // === 2. 调用渠道退款（事务外） ===
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(paymentOrder.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        refundRequest.setAmount(refundAmount);
        refundRequest.setReason(reason);
        RefundResponse refundResponse;
        try {
            refundResponse = paymentChannel.refund(refundRequest);
        } catch (Exception e) {
            log.error("【严重】订单取消自动退款渠道调用异常，需人工处理！orderId={}, paymentOrderId={}, amount={}",
                    orderId, paymentOrder.getId(), refundAmount, e);
            return false;
        }
        if (refundResponse == null || !refundResponse.isSuccess()) {
            log.error("【严重】订单取消自动退款被渠道拒绝，需人工处理！orderId={}, paymentOrderId={}, errorMsg={}",
                    orderId, paymentOrder.getId(),
                    refundResponse != null ? refundResponse.getErrorMsg() : "无响应");
            return false;
        }

        // === 3. 事务内本地落库（行锁二次校验 + CAS 防覆盖） ===
        final BigDecimal fRefundAmount = refundAmount;
        final String fReason = (reason != null && !reason.trim().isEmpty()) ? reason : "订单自动退款";
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                // 重新查询支付单（防并发退款）
                PaymentOrder latest = paymentOrderService.getById(paymentOrder.getId());
                if (latest == null || !STATUS_SUCCESS.equals(latest.getStatus())) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                // 事务内二次累计退款校验（SELECT ... FOR UPDATE 锁支付单行，阻塞并发退款）
                BigDecimal lockedAmount = paymentOrderMapper.selectPaymentAmountForUpdate(latest.getId(), STATUS_SUCCESS);
                BigDecimal latestAmount = latest.getAmount();
                if (lockedAmount == null || latestAmount == null) {
                    throw new CustomException("支付金额异常，退款失败");
                }
                if (lockedAmount.compareTo(latestAmount) != 0) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                BigDecimal refunded = refundRecordService.sumRefundedAmount(latest.getId());
                if (refunded.add(fRefundAmount).compareTo(latestAmount) > 0) {
                    throw new CustomException("累计退款金额超过支付金额（已退：" + refunded + "元）");
                }
                // 创建退款记录并标记成功（渠道已确认退款）
                RefundRecord record = refundRecordService.createRefund(latest.getId(), fRefundAmount, fReason);
                refundRecordService.markRefundSuccess(record.getRefundNo());
                // 全额退款时：支付单 SUCCESS -> REFUND + 业务订单联动为已退款(6)
                boolean isFull = refunded.add(fRefundAmount).compareTo(latestAmount) == 0;
                if (isFull) {
                    boolean poUpdated = paymentOrderService.lambdaUpdate()
                            .eq(PaymentOrder::getId, latest.getId())
                            .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                            .set(PaymentOrder::getStatus, STATUS_REFUND)
                            .set(PaymentOrder::getUpdateTime, LocalDateTime.now())
                            .update();
                    if (!poUpdated) {
                        throw new CustomException("支付单状态已变更，退款失败");
                    }
                    Orders order = orderService.getById(latest.getOrderId());
                    if (order != null) {
                        Integer curStatus = order.getStatus();
                        if (curStatus != null && Arrays.asList(
                                Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING, Orders.STATUS_COMPLETED).contains(curStatus)) {
                            LambdaUpdateWrapper<Orders> orderUpdateWrapper = new LambdaUpdateWrapper<>();
                            orderUpdateWrapper.eq(Orders::getId, order.getId())
                                    .eq(Orders::getStatus, curStatus);
                            Orders updateEntity = new Orders();
                            updateEntity.setStatus(Orders.STATUS_REFUNDED);
                            updateEntity.setUpdateTime(LocalDateTime.now());
                            if (orderService.update(updateEntity, orderUpdateWrapper)) {
                                try {
                                    memberRewardService.reverseRewards(latest.getOrderId(), latest.getTenantId());
                                    log.info("[会员权益回退] 自动退款触发权益回退: orderId={}, tenantId={}",
                                            latest.getOrderId(), latest.getTenantId());
                                } catch (Exception e) {
                                    log.error("[会员权益回退] 自动退款后权益回退失败，需人工核查: orderId={}",
                                            latest.getOrderId(), e);
                                }
                                log.info("自动退款成功联动更新订单: orderId={}, orderStatus=已退款", latest.getOrderId());
                            } else {
                                log.warn("订单状态已变更，跳过联动退款更新: orderId={}, expectedStatus={}",
                                        latest.getOrderId(), curStatus);
                            }
                        } else if (curStatus != null && Objects.equals(curStatus, Orders.STATUS_REFUNDED)) {
                            log.info("订单已为已退款状态，幂等跳过联动更新: orderId={}", latest.getOrderId());
                        } else {
                            log.warn("订单状态不允许退款流转，跳过联动更新: orderId={}, currentStatus={}",
                                    latest.getOrderId(), curStatus);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            // 渠道已退款但本地落库失败——资金已出、数据未同步，必须告警人工核对。
            // catch Exception 覆盖 CustomException（业务校验）+ DataAccessException（DB 异常）等所有本地失败，
            // 避免渠道已退款却因非业务异常漏留对账痕迹导致资金流失。
            // 1. 降级持久化对账待办痕迹（独立事务，供 RefundReconcileTask 扫描）
            try {
                refundRecordService.recordReconcileTrace(paymentOrder.getId(), refundAmount, fReason);
            } catch (Exception traceEx) {
                log.error("【严重】渠道退款成功但本地落库失败，对账痕迹持久化也失败: orderId={}, paymentOrderId={}, refundAmount={}",
                        orderId, paymentOrder.getId(), refundAmount, traceEx);
            }
            // 2. 主日志告警
            log.error("【严重】渠道退款成功但本地数据更新失败，需人工核对对账！orderId={}, paymentOrderId={}, refundAmount={}",
                    orderId, paymentOrder.getId(), refundAmount, e);
            return false;
        }
        log.info("订单自动退款成功: orderId={}, paymentOrderId={}, refundAmount={}", orderId, paymentOrder.getId(), refundAmount);
        return true;
    }
}
