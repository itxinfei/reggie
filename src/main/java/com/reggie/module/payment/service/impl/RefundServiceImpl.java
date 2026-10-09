package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.enums.RefundStatus;
import com.reggie.module.dashboard.service.DashboardService;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderStockRefundService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.reggie.module.payment.model.PaymentOrder.STATUS_REFUND;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;

/**
 * 退款服务实现：退款编排的唯一实现。
 * <p>
 * 承载三类退款发起，共用同一套私有编排原语（Redis 锁 / 渠道 HTTP / 事务化落库 / 全额联动 / 对账埋点），
 * 消除历史上 PaymentController 与本类各持一份编排导致的口径漂移（审查 §2.2-10）：
 * <ul>
 *   <li>{@link #refundByOrder}：按业务订单全额退款（订单取消/拒单/拼团未成团自动退款）；</li>
 *   <li>{@link #refundByPaymentOrder}：员工按支付单手动退款（支持部分退款，端点 {@code /payment/refund}）；</li>
 *   <li>{@link #executeUserRefundByRecord}：售后单审核通过后触发渠道退款（复用既有售后单，端点
 *       {@code /payment/refund/user/execute}）。</li>
 * </ul>
 * <b>事务边界（P0 修正）</b>：本类<b>不再有任何方法级/类级 {@code @Transactional}</b>。编排严格按
 * 「Redis 锁 → 金额/状态校验 → 渠道 HTTP（<b>无事务</b>，外部 HTTP 绝不被事务包裹） → 落库段用编程式
 * {@link TransactionTemplate} 事务（只包 DB 写） → 全额联动与退款记录同事务」执行，杜绝原
 * {@code @Transactional(REQUIRES_NEW)} 把整个方法（含 Redis 锁等待 + 渠道 HTTP）包进长事务的资损定性错误。
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
    private com.reggie.module.subsidy.service.SubsidyService subsidyService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @Autowired
    private MemberRewardService memberRewardService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 全额退款时同步回补菜品+原料库存（幂等，收敛自 PaymentController） */
    @Autowired
    private OrderStockRefundService orderStockRefundService;

    /** 退款成功后清除 Dashboard 概览缓存（收敛自 PaymentController，保持端点行为一致） */
    @Autowired
    private DashboardService dashboardService;

    /** 分布式锁过期时间（毫秒）：覆盖一次渠道退款 HTTP 调用耗时 */
    private static final long LOCK_TTL_MS = 30 * 1000L; // 30秒

    /** 渠道退款处理中统一提示（与退款回调终态化配合；手动/自动/售后三流一致） */
    private static final String MSG_PROCESSING = "退款申请已提交，渠道处理中，最终结果以微信退款通知为准";
    /** 退款终态成功提示 */
    private static final String MSG_SUCCESS = "退款成功";
    /** 线下支付本地记账退款成功提示 */
    private static final String MSG_OFFLINE_OK = "退款成功（线下支付，已记录手动退款）";
    /** 售后单已退款成功的幂等提示 */
    private static final String MSG_ALREADY_SUCCESS = "该售后单已退款成功，请勿重复操作";

    /** 渠道退款结果状态 */
    private enum ChannelState {
        /** 同步成功，可继续本地落库 */
        SUCCESS,
        /** 渠道受理处理中，终态以退款回调为准 */
        PROCESSING,
        /** 渠道拒绝（钱未出，已留对账待办） */
        REJECTED,
        /** 渠道调用异常（钱未出，已留对账待办） */
        EXCEPTION
    }

    /** 渠道退款结果：状态 + 拒绝时渠道错误信息（仅 REJECTED 非空，供上层组装用户可见消息） */
    private static final class ChannelOutcome {
        final ChannelState state;
        final String errMsg;

        ChannelOutcome(ChannelState state, String errMsg) {
            this.state = state;
            this.errMsg = errMsg;
        }
    }

    /**
     * 退款 by order（按业务订单全额退款）。
     * <p>无方法级事务：渠道 HTTP 在事务外，落库段由 {@link #persistRefundInTransaction} 的
     * REQUIRES_NEW 编程式事务保证原子（供外层事务内的取消/拒单/拼团调用，独立提交不污染外层）。</p>
     * @param orderId 参数 orderId
     * @param reason 参数 reason
     * @return 返回结果
     */
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
        Long paymentOrderId = paymentOrder.getId();
        BigDecimal paymentAmount = paymentOrder.getAmount();
        if (paymentAmount == null) {
            log.warn("订单自动退款跳过：支付单金额缺失，数据异常 orderId={}, paymentOrderId={}",
                    orderId, paymentOrderId);
            return false;
        }

        // 离线支付通道（现金/银行卡/储值/货到付款等）无法走渠道 API 退款：线下支付的退款由人工完成，
        // 系统仅做本地记账闭环（标记支付单 REFUND + 订单 REFUNDED + 回退权益），避免 getChannel 抛
        // “不支持的支付通道”导致自动退款失败、已支付订单取消/拒单后卡死在待接单（Defect B）。
        if (!isOnlineChannel(paymentOrder.getChannel())) {
            return doLocalManualRefund(paymentOrder, paymentAmount, reason, orderId, paymentOrderId);
        }

        // === 2. Redis 分布式锁串行化同一支付单的退款发起 ===
        // P0 修复（双重扣款根因）：此前"查询校验（阶段1）→ 渠道调用（阶段2）→ FOR UPDATE 落库（阶段3）"三段式中，
        // 渠道调用发生在 FOR UPDATE 行锁之前。两个并发退款都通过阶段1校验后双双调用渠道，
        // 渠道重复扣款后本地 FOR UPDATE 只能拦住第二个的落库——资金已出、记录未建，形成双重扣款。
        // 现于"校验后、渠道调用前"加 Redis 锁 payment:refund:lock:{paymentOrderId}，锁内串行化：
        // 重新查询支付单状态（非 SUCCESS 拒绝）+ 生成 outRequestNo（渠道幂等键）+ 渠道调用，finally 释放锁。
        // fail-open：Redis 不可用时降级到 DB FOR UPDATE + 渠道 out_request_no 幂等兜底（单实例 ConcurrentHashMap 仍有保护）。
        String lockKey = "payment:refund:lock:" + paymentOrderId;
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            log.warn("退款分布式锁获取失败，降级 DB+渠道幂等兜底: orderId={}, paymentOrderId={}", orderId, paymentOrderId);
        }
        try {
            // === 2.1 锁内重查支付单（防止锁前已退款/状态已变更，非 SUCCESS 拒绝） ===
            PaymentOrder latestCheck = paymentOrderService.getById(paymentOrderId);
            if (latestCheck == null || !STATUS_SUCCESS.equals(latestCheck.getStatus())) {
                log.info("订单自动退款跳过：支付单状态已变更（锁内重查） orderId={}, paymentOrderId={}",
                        orderId, paymentOrderId);
                return false;
            }
            BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrderId);
            if (alreadyRefunded.compareTo(latestCheck.getAmount()) >= 0) {
                log.info("订单自动退款幂等跳过：已全额退款 orderId={}, paymentOrderId={}", orderId, paymentOrderId);
                return false;
            }
            BigDecimal refundAmount = latestCheck.getAmount().subtract(alreadyRefunded);

            // === 2.2 生成退款单号并作为渠道幂等键（out_request_no） ===
            // 微信/支付宝以同一商户退款单号做退款幂等去重：重复请求只退款一次，
            // 防"本地落库失败后重试/并发退款"造成的双重扣款。
            String refundNo = generateRefundNo();

            // === 2.3 调用渠道退款（事务外，外部 HTTP 不被事务包裹；新建 PROCESSING 记录独立提交） ===
            ChannelOutcome outcome = invokeChannelRefund(latestCheck, refundAmount, reason, refundNo,
                    orderId, true, false);
            if (outcome.state == ChannelState.REJECTED || outcome.state == ChannelState.EXCEPTION) {
                return false;
            }
            if (outcome.state == ChannelState.PROCESSING) {
                // 渠道处理中（PROCESSING 已登记）：退款已发起、终态以退款异步回调为准，
                // 此处不做支付单/订单/库存/积分联动，取消流程可继续。
                log.info("订单取消退款已受理处理中，等待渠道回调: orderId={}, refundNo={}", orderId, refundNo);
                return true;
            }

            // === 3. 事务内本地落库（等价抽取，行锁二次校验 + CAS 防覆盖；REQUIRES_NEW 独立提交） ===
            final Long fPaymentOrderId = paymentOrderId;
            final BigDecimal fRefundAmount = refundAmount;
            final String fReason = (reason != null && !reason.trim().isEmpty()) ? reason : "订单自动退款";
            try {
                persistRefundInTransaction(paymentOrderId, refundAmount, fReason, refundNo, true);
            } catch (Exception e) {
                // 渠道已退款但本地落库失败（等价抽取）
                return handlePersistRefundFailure(fPaymentOrderId, fRefundAmount, fReason, orderId, e);
            }
            log.info("订单自动退款成功: orderId={}, paymentOrderId={}, refundAmount={}", orderId, fPaymentOrderId,
                    fRefundAmount);
            return true;
        } finally {
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 判断是否为可走渠道 API 的在线支付通道（微信/支付宝）。
     * 现金/银行卡/储值/货到付款等线下通道无法调渠道退款 API，须走本地手动退款记账。
     *
     * @param channel 渠道
     * @return 是否在线通道
     */
    public boolean isOnlineChannel(String channel) {
        if (channel == null) {
            return false;
        }
        String c = channel.toUpperCase();
        return "WECHAT".equals(c) || "ALIPAY".equals(c);
    }

    /**
     * 线下支付通道本地手动退款（Defect B 修复）。
     * <p>现金/银行卡/储值/货到付款等线下支付的退款由人工完成，系统仅做本地记账闭环：
     * 创建退款记录（成功）→ 支付单 SUCCESS→REFUND → 订单联动为已退款(6) 并回退会员权益。
     * 不调用任何渠道 API，避免 paymentChannelFactory.getChannel 抛“不支持的支付通道”导致自动退款失败、
     * 已支付订单取消/拒单后卡死在“待接单(2)”（既没取消也没退款）。
     * 幂等：支付单已非 SUCCESS 或累计已退足则直接返回成功。</p>
     *
     * @return 是否成功
     */
    private boolean doLocalManualRefund(PaymentOrder paymentOrder, BigDecimal amount, String reason,
            Long orderId, Long paymentOrderId) {
        final String fReason = (reason != null && !reason.trim().isEmpty()) ? reason : "线下支付手动退款";
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            txTemplate.execute(status -> {
                PaymentOrder latest = paymentOrderService.getById(paymentOrderId);
                if (latest == null || !STATUS_SUCCESS.equals(latest.getStatus())) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                BigDecimal refunded = refundRecordService.sumRefundedAmount(latest.getId());
                if (refunded.compareTo(latest.getAmount()) >= 0) {
                    return null; // 已全额退款，幂等跳过
                }
                RefundRecord record = refundRecordService.createRefund(latest.getId(), amount, fReason,
                        generateRefundNo());
                refundRecordService.markRefundSuccess(record.getRefundNo());
                paymentOrderService.lambdaUpdate()
                        .eq(PaymentOrder::getId, latest.getId())
                        .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                        .set(PaymentOrder::getStatus, STATUS_REFUND)
                        .set(PaymentOrder::getUpdateTime, LocalDateTime.now())
                        .update();
                Orders order = orderService.getById(latest.getOrderId());
                if (order != null) {
                    updateOrderOnFullRefund(order, latest);
                }
                // 餐补渠道退款：本地记账后同步回充餐补余额（幂等键=退款单号，重复触发不重复回充）
                if ("SUBSIDY".equalsIgnoreCase(latest.getChannel()) && order != null) {
                    subsidyService.refundForOrder(latest.getTenantId(), order.getUserId(),
                            order.getId(), amount, record.getRefundNo());
                }
                return null;
            });
            log.info("[线下退款] 本地记账退款成功: orderId={}, channel={}, amount={}", orderId,
                    paymentOrder.getChannel(), amount);
            return true;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[线下退款] 本地记账退款失败，需人工核查: orderId={}, paymentOrderId={}, amount={}",
                    orderId, paymentOrderId, amount, e);
            return false;
        }
    }

    /**
     * 按支付单ID执行线下支付本地手动退款（供员工手动退款/售后退款路径复用）。
     *
     * @return 是否成功
     */
    @Override
    public boolean refundOfflineByPaymentOrderId(Long paymentOrderId, BigDecimal amount, String reason) {
        if (paymentOrderId == null) {
            return false;
        }
        PaymentOrder po = paymentOrderService.getById(paymentOrderId);
        if (po == null || !STATUS_SUCCESS.equals(po.getStatus())) {
            return false;
        }
        return doLocalManualRefund(po, amount, reason, po.getOrderId(), paymentOrderId);
    }

    /**
     * 调用渠道退款（<b>无事务</b>）。返回渠道结果 {@link ChannelOutcome}：SUCCESS=同步成功（可继续落库）；
     * PROCESSING=渠道受理处理中（终态以回调为准）；REJECTED=渠道拒绝；EXCEPTION=渠道调用异常
     * （REJECTED/EXCEPTION 均钱未出，已留 [对账待办] 痕迹供 RefundReconcileTask 扫描人工退款）。
     * <p>PROCESSING 分支：{@code registerNewProcessingRecord=true}（手动/自动退款，需新建财务退款记录）时，
     * 用 REQUIRES_NEW 独立事务登记 PROCESSING 记录——即便外层业务事务（拼团/状态流转）回滚，该记录必须存活
     * 以待退款回调定终态；{@code =false}（售后退款，复用既有售后单）时不新建记录（同号插入会撞 uk_refund_no）。</p>
     * <p>{@code afterSale=true} 时对账埋点文案切换为"售后退款"口径，与原 PaymentController.executeUserRefund
     * 的埋点口径保持一致（收敛不改变可观测痕迹文本）。</p>
     *
     * @param orderId 业务订单ID（仅日志用）
     */
    private ChannelOutcome invokeChannelRefund(PaymentOrder po, BigDecimal refundAmount, String reason,
            String refundNo, Long orderId, boolean registerNewProcessingRecord, boolean afterSale) {
        final Long paymentOrderId = po.getId();
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(po.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(po.getChannelTradeNo());
        refundRequest.setAmount(refundAmount);
        refundRequest.setReason(reason);
        refundRequest.setOutRequestNo(refundNo);
        RefundResponse refundResponse;
        try {
            refundResponse = paymentChannel.refund(refundRequest);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("【严重】退款渠道调用异常，需人工处理！orderId={}, paymentOrderId={}, amount={}",
                    orderId, paymentOrderId, refundAmount, e);
            // 渠道调用异常（钱未出）——留对账待办痕迹，供 RefundReconcileTask 扫描告警人工退款（M1）
            recordReconcileTraceSafely(po, refundAmount, afterSale
                    ? "[对账待办]售后退款渠道调用异常：" + e.getMessage()
                    : "[对账待办]渠道退款调用异常待人工");
            return new ChannelOutcome(ChannelState.EXCEPTION, null);
        }
        if (refundResponse != null && refundResponse.isProcessing()) {
            // 渠道同步 PROCESSING：终态以退款回调为准，此处不做支付单/订单/库存/积分联动
            log.info("退款已受理处理中：orderId={}, refundNo={}, afterSale={}", orderId, refundNo, afterSale);
            if (registerNewProcessingRecord) {
                // REQUIRES_NEW：渠道已受理（钱在路上），PROCESSING 记录独立提交，终态以退款回调为准
                try {
                    TransactionTemplate regTx = new TransactionTemplate(transactionManager);
                    regTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    regTx.execute(tx -> {
                        refundRecordService.createRefund(paymentOrderId, refundAmount, reason, refundNo);
                        refundRecordService.markRefundProcessing(refundNo);
                        return null;
                    });
                } catch (Exception ex) {
                    // 渠道已受理（钱在路上）但本地登记失败：留对账待办，禁止重复发起以免重复退款
                    log.error("【严重】退款处理中本地登记失败，需人工核对：orderId={}, refundNo={}",
                            orderId, refundNo, ex);
                    recordReconcileTraceSafely(po, refundAmount,
                            "[对账待办]退款处理中本地登记失败：" + ex.getMessage());
                    return new ChannelOutcome(ChannelState.EXCEPTION, null);
                }
            }
            return new ChannelOutcome(ChannelState.PROCESSING, null);
        }
        if (refundResponse == null || !refundResponse.isSuccess()) {
            String errMsg = refundResponse != null ? refundResponse.getErrorMsg() : "无响应";
            log.error("【严重】退款被渠道拒绝，需人工处理！orderId={}, paymentOrderId={}, errorMsg={}",
                    orderId, paymentOrderId, errMsg);
            // 渠道拒绝（钱未出）——留对账待办痕迹，供 RefundReconcileTask 扫描告警人工退款（M1）
            recordReconcileTraceSafely(po, refundAmount, afterSale
                    ? "[对账待办]售后退款渠道被拒绝：" + errMsg
                    : "[对账待办]渠道退款被拒绝待人工：" + errMsg);
            return new ChannelOutcome(ChannelState.REJECTED, errMsg);
        }
        return new ChannelOutcome(ChannelState.SUCCESS, null);
    }

    /**
     * 渠道已退款但本地落库失败时的告警与对账痕迹处理（等价抽取，降低方法长度）。
     *
     * @return 恒定返回 false（供调用方直接 return）
     */
    private boolean handlePersistRefundFailure(Long paymentOrderId, BigDecimal refundAmount, String reason,
            Long orderId, Exception e) {
        // 渠道已退款但本地落库失败——资金已出、数据未同步，必须告警人工核对。
        // catch Exception 覆盖 CustomException（业务校验）+ DataAccessException（DB 异常）等所有本地失败，
        // 避免渠道已退款却因非业务异常漏留对账痕迹导致资金流失。
        // 1. 降级持久化对账待办痕迹（独立事务，供 RefundReconcileTask 扫描）
        try {
            refundRecordService.recordReconcileTrace(paymentOrderId, refundAmount, reason);
        } catch (Exception traceEx) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("【严重】渠道退款成功但本地落库失败，对账痕迹持久化也失败: orderId={}, paymentOrderId={}, refundAmount={}",
                    orderId, paymentOrderId, refundAmount, traceEx);
        }
        // 2. 主日志告警
        log.error("【严重】渠道退款成功但本地数据更新失败，需人工核对对账！orderId={}, paymentOrderId={}, refundAmount={}",
                orderId, paymentOrderId, refundAmount, e);
        return false;
    }

    /**
     * 事务内本地落库：行锁二次校验 + 创建退款记录 + 全额退款联动（等价抽取，降低方法长度）。
     *
     * @param propagateRequiresNew true=REQUIRES_NEW（供外层事务内的自动退款调用，独立提交不污染外层——修复
     *                             拼团退款 catch 后仍 UnexpectedRollback）；false=REQUIRED（员工手动退款端点
     *                             无外层事务；测试的类级 @Transactional 事务内需可见并随测试回滚）
     */
    private void persistRefundInTransaction(Long paymentOrderId, BigDecimal refundAmount, String fReason,
            String refundNo, boolean propagateRequiresNew) {
        final BigDecimal fRefundAmount = refundAmount;
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(propagateRequiresNew
                ? TransactionDefinition.PROPAGATION_REQUIRES_NEW : TransactionDefinition.PROPAGATION_REQUIRED);
        txTemplate.execute(status -> {
            // 重新查询支付单（防并发退款）
            PaymentOrder latest = paymentOrderService.getById(paymentOrderId);
            if (latest == null || !STATUS_SUCCESS.equals(latest.getStatus())) {
                throw new CustomException("支付单状态已变更，退款失败");
            }
            // 事务内二次累计退款校验（SELECT ... FOR UPDATE 锁支付单行，阻塞并发退款）
            BigDecimal lockedAmount = paymentOrderMapper.selectPaymentAmountForUpdate(latest.getId(),
                    STATUS_SUCCESS);
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
            // 创建退款记录并标记成功（渠道已确认退款）。refundNo 提前生成作为渠道幂等键，
            // 此处复用同一单号，保证本地记录与渠道 out_request_no 一一对应。
            RefundRecord record = refundRecordService.createRefund(latest.getId(), fRefundAmount, fReason, refundNo);
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
                    updateOrderOnFullRefund(order, latest);
                }
            }
            return null;
        });
    }

    /**
     * 全额退款时联动更新业务订单状态并回退会员权益/库存（唯一实现，收敛自 PaymentController 的漂移副本）。
     * <p>状态白名单为两份副本的<b>并集</b>：已下单(2)/配送中(3)/已完成(4)/已取消(5)。
     * 保留已取消(5)——支付回调晚于取消到达 / 取消后自动退款场景须允许 5→6（PaymentRefundFlowIntegrationTest
     * 的 testNotifyWhenOrderAlreadyCancelled 依赖此）；并入 Controller 版独有的库存回补 restoreForOrder——
     * 员工手动全额退款必须回补菜品/原料库存（restoreForOrder 幂等，自动退款路径此前已在取消时回补过则为空操作）。</p>
     *
     * @param order 关联业务订单
     * @param latest 支付单
     */
    private void updateOrderOnFullRefund(Orders order, PaymentOrder latest) {
        Integer curStatus = order.getStatus();
        // 允许已取消(5)流转：支付回调晚于取消到达时，订单此前已被置5（库存/权益在取消时已处理），
        // 退款成功后同样应转为已退款6，避免"订单已取消 + 支付已退款"的状态矛盾
        if (curStatus != null && Arrays.asList(
                Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING, Orders.STATUS_COMPLETED,
                Orders.STATUS_CANCELLED).contains(curStatus)) {
            LambdaUpdateWrapper<Orders> orderUpdateWrapper = new LambdaUpdateWrapper<>();
            orderUpdateWrapper.eq(Orders::getId, order.getId()).eq(Orders::getStatus, curStatus);
            Orders updateEntity = new Orders();
            updateEntity.setStatus(Orders.STATUS_REFUNDED);
            updateEntity.setUpdateTime(LocalDateTime.now());
            if (!orderService.update(updateEntity, orderUpdateWrapper)) {
                log.warn("订单状态已变更，跳过联动退款更新: orderId={}, expectedStatus={}", latest.getOrderId(), curStatus);
                return;
            }
            // 会员权益回退（积分 + 优惠券）+ 菜品/原料库存回补（幂等，各自 best-effort，失败由补偿任务兜底）
            revertMemberRewardsAndStock(latest.getOrderId(), latest.getTenantId());
            log.info("退款成功联动更新订单: orderId={}, orderStatus=已退款", latest.getOrderId());
        } else if (curStatus != null && Objects.equals(curStatus, Orders.STATUS_REFUNDED)) {
            log.info("订单已为已退款状态，幂等跳过联动更新: orderId={}", latest.getOrderId());
        } else {
            log.warn("订单状态不允许退款流转，跳过联动更新: orderId={}, currentStatus={}", latest.getOrderId(), curStatus);
        }
    }

    /**
     * 全额退款联动的会员权益回退 + 库存回补（收敛自 PaymentController 两份副本的公共部分）。
     * 各自 try/catch 兜底、仅记录日志、不阻断退款主流程（失败由库存补偿任务 / 人工核查兜底）。
     */
    private void revertMemberRewardsAndStock(Long orderId, Long tenantId) {
        try {
            memberRewardService.reverseRewards(orderId, tenantId);
            log.info("[会员权益回退] 退款触发权益回退: orderId={}, tenantId={}", orderId, tenantId);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[会员权益回退] 退款后权益回退失败，需人工核查: orderId={}", orderId, e);
        }
        try {
            orderStockRefundService.restoreForOrder(orderId);
        } catch (Exception e) {
            log.error("[库存回补] 退款触发库存回补异常，待补偿任务兜底: orderId={}", orderId, e);
        }
    }

    /**
     * 尝试获取分布式锁（退款发起场景，与 {@code PaymentOrderServiceImpl.tryLock} 同模式）。
     * @param lockKey 锁Key
     * @return 锁值（UUID），Redis 不可用或被占用返回 null（降级 DB+渠道幂等兜底）
     */
    private String tryLock(String lockKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("退款获取分布式锁失败，降级 DB+渠道幂等兜底: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁（Lua 脚本原子操作：比对锁值后才删除，防止误删他人锁）。
     * @param lockKey 锁Key
     * @param lockValue 锁值（UUID）
     */
    private void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("退款释放分布式锁失败: {}", lockKey, e);
        }
    }

    /**
     * 生成退款流水号：RF + 时间戳 + UUID 前8位（保证唯一性，可作渠道退款幂等键 out_request_no）。
     */
    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 安全持久化对账待办痕迹（渠道退款失败场景：钱未出，待人工退款）。
     * <p>
     * 复用 {@link RefundRecordService#recordReconcileTrace} 的 REQUIRES_NEW 独立事务机制，
     * reason 以 {@code [对账待办]} 前缀标识，供 {@link com.reggie.module.payment.task.RefundReconcileTask} 扫描告警。
     * trace 自身失败仅 log.error，不阻断调用方流程。
     * </p>
     */
    private void recordReconcileTraceSafely(PaymentOrder paymentOrder, BigDecimal amount, String reason) {
        try {
            refundRecordService.recordReconcileTrace(paymentOrder.getId(), amount, reason);
        } catch (Exception traceEx) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("【严重】渠道退款失败对账痕迹持久化也失败，需人工核查: paymentOrderId={}, amount={}",
                    paymentOrder.getId(), amount, traceEx);
        }
    }

    // ==================================================================================
    // 员工手动退款（端点 /payment/refund）与售后执行退款（端点 /payment/refund/user/execute）
    // 编排：自 PaymentController 收敛而来，与 refundByOrder 共用同一套私有编排原语
    //（Redis 锁 / 渠道 HTTP / 事务化落库 / 全额联动 / 对账埋点），彻底消除双实现漂移。
    // 契约：方法返回 String=成功文案（R.success），抛 CustomException=失败文案（Controller catch→R.error），
    //       与原 Controller 各分支的返回文案逐一对应。
    // ==================================================================================

    /**
     * 员工按支付单手动退款（支持部分退款）。收敛自 PaymentController.refund + validateRefundRequest +
     * callChannelRefund + persistRefundTransactionally。落库段用 REQUIRED 编程式事务（员工端点无外层事务）。
     *
     * @param tenantId      当前会话租户（Controller 传入；null 视为越权，fail-closed）
     * @param paymentOrderId 支付单ID
     * @param refundAmount  本次退款金额（<= 支付金额，<= 剩余可退额）
     * @param reason        退款原因
     * @return 成功文案（终态成功 / 处理中 / 线下记账成功）
     * @throws CustomException 校验失败、渠道拒绝/异常、本地落库失败
     */
    @Override
    public String refundByPaymentOrder(Long tenantId, Long paymentOrderId, BigDecimal refundAmount, String reason) {
        // === 1. 校验阶段（无事务，避免长事务持有 DB 锁） ===
        PaymentOrder paymentOrder = paymentOrderService.getById(paymentOrderId);
        if (paymentOrder == null) {
            throw new CustomException("支付订单不存在");
        }
        BigDecimal paymentAmount = paymentOrder.getAmount();
        // 租户归属校验（fail-closed，兜底租户拦截器在 tenantId 为 null 时跳过过滤的极端情况）
        if (tenantId == null || !tenantId.equals(paymentOrder.getTenantId())) {
            log.warn("退款越权拦截：paymentOrderId={}, orderTenant={}, curTenant={}",
                    paymentOrderId, paymentOrder.getTenantId(), tenantId);
            throw new CustomException("无权操作其他租户的支付订单");
        }
        // 支付单状态机：仅 SUCCESS 可退
        if (!STATUS_SUCCESS.equals(paymentOrder.getStatus())) {
            throw new CustomException("支付订单状态不允许退款（当前状态：" + paymentOrder.getStatus() + "）");
        }
        // 金额校验 fail-closed：支付单金额缺失属数据异常，拒绝而非跳过（否则单次/累计两道防线同时失效）
        if (paymentAmount == null) {
            throw new CustomException("支付金额异常，无法退款");
        }
        if (refundAmount == null || refundAmount.compareTo(paymentAmount) > 0) {
            throw new CustomException("退款金额不能大于支付金额（支付金额：" + paymentAmount + "元）");
        }
        // 累计退款金额粗校验（事务内还会二次校验防并发）
        BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrder.getId());
        if (alreadyRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
            throw new CustomException("累计退款金额超过支付金额（已退：" + alreadyRefunded + "元）");
        }

        // === 1.5 离线通道：本地记账闭环，不调渠道 API（避免 getChannel 抛"不支持的支付通道"导致 500） ===
        if (!isOnlineChannel(paymentOrder.getChannel())) {
            boolean offlineOk = doLocalManualRefund(paymentOrder, refundAmount, reason,
                    paymentOrder.getOrderId(), paymentOrder.getId());
            if (!offlineOk) {
                throw new CustomException("线下支付退款记账失败，需人工核查");
            }
            clearDashboardCache();
            log.info("[退款] 线下支付本地记账退款成功: paymentOrderId={}, channel={}, amount={}",
                    paymentOrder.getId(), paymentOrder.getChannel(), refundAmount);
            return MSG_OFFLINE_OK;
        }

        // === 2. Redis 锁串行化同一支付单退款发起 + 渠道 HTTP（事务外） ===
        String lockKey = "payment:refund:lock:" + paymentOrderId;
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            log.warn("退款分布式锁获取失败，降级 DB+渠道幂等兜底: paymentOrderId={}", paymentOrderId);
        }
        try {
            // 锁内重查支付单（防锁前已退款/状态变更，非 SUCCESS 拒绝）
            PaymentOrder lockedOrder = paymentOrderService.getById(paymentOrderId);
            if (lockedOrder == null || !STATUS_SUCCESS.equals(lockedOrder.getStatus())) {
                log.warn("退款重查支付单状态已变更，拒绝退款：paymentOrderId={}, status={}", paymentOrderId,
                        lockedOrder != null ? lockedOrder.getStatus() : "null");
                throw new CustomException("支付订单状态已变更，请刷新后重试");
            }
            BigDecimal lockedRefunded = refundRecordService.sumRefundedAmount(paymentOrderId);
            if (lockedRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
                throw new CustomException("累计退款金额超过支付金额（已退：" + lockedRefunded + "元）");
            }
            // 生成退款单号作为渠道幂等键 out_request_no
            String refundNo = generateRefundNo();
            // 渠道退款（无事务；同步成功即继续落库，处理中登记 PROCESSING 独立提交，拒绝/异常留对账待办）
            ChannelOutcome outcome = invokeChannelRefund(lockedOrder, refundAmount, reason, refundNo,
                    lockedOrder.getOrderId(), true, false);
            if (outcome.state == ChannelState.PROCESSING) {
                return MSG_PROCESSING;
            }
            if (outcome.state == ChannelState.REJECTED) {
                throw new CustomException("退款失败: " + outcome.errMsg);
            }
            if (outcome.state == ChannelState.EXCEPTION) {
                throw new CustomException("退款渠道调用失败，请稍后重试");
            }
            // === 3. 事务内本地落库（渠道已退款成功，本地必须落库；REQUIRED，员工端点无外层事务） ===
            try {
                persistRefundInTransaction(paymentOrderId, refundAmount, reason, refundNo, false);
            } catch (Exception e) {
                // 渠道已退款但本地落库失败——资金已出、数据未同步，必须留对账痕迹 + 告警
                handlePersistRefundFailure(paymentOrderId, refundAmount, reason, lockedOrder.getOrderId(), e);
                throw new CustomException("退款已提交渠道但本地更新失败，请联系管理员核对");
            }
            clearDashboardCache();
            log.info("退款成功: paymentOrderId={}, refundAmount={}", paymentOrderId, refundAmount);
            return MSG_SUCCESS;
        } finally {
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 售后单审核通过后触发渠道退款（复用既有售后单，不新建同号财务记录）。收敛自
     * PaymentController.executeUserRefund + doChannelRefund + persistUserRefund，落库段改为编程式事务
     *（原实现无事务包裹，退款记录与全额联动可部分失败）。
     *
     * @param tenantId 当前会话租户
     * @param refundId 售后记录ID（状态须为 PROCESSING）
     * @return 成功文案（终态成功 / 处理中 / 已退款的幂等提示 / 渠道已处理请核对）
     * @throws CustomException 记录不存在、越权、状态不符、无有效支付单、渠道拒绝/异常
     */
    @Override
    public String executeUserRefundByRecord(Long tenantId, Long refundId) {
        RefundRecord record = refundRecordService.getById(refundId);
        if (record == null) {
            throw new CustomException("售后记录不存在");
        }
        if (tenantId != null && !tenantId.equals(record.getTenantId())) {
            throw new CustomException("无权操作其他租户的售后记录");
        }
        if (RefundStatus.SUCCESS.getCode().equals(record.getStatus())) {
            return MSG_ALREADY_SUCCESS;
        }
        if (!RefundStatus.PROCESSING.getCode().equals(record.getStatus())) {
            throw new CustomException("该售后单当前状态不支持退款执行（需先审核通过）");
        }
        Orders order = orderService.getById(record.getOrderId());
        if (order == null) {
            throw new CustomException("关联订单不存在");
        }
        // 查找该订单的已成功支付单（最新一笔）
        PaymentOrder paymentOrder = paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getOrderId, order.getId())
                .eq(PaymentOrder::getTenantId, tenantId)
                .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                .orderByDesc(PaymentOrder::getPaidTime)
                .last("LIMIT 1")
                .one();
        if (paymentOrder == null) {
            throw new CustomException("未找到该订单的有效支付单，无法退款");
        }
        BigDecimal refundAmount = record.getAmount();
        BigDecimal paymentAmount = paymentOrder.getAmount();
        if (paymentAmount == null) {
            throw new CustomException("支付金额异常，无法退款");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("售后退款金额异常，无法退款");
        }

        // Redis 锁（与手动/自动退款同一 payment:refund:lock:{id} 键，串行化同一支付单）
        String lockKey = "payment:refund:lock:" + paymentOrder.getId();
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            log.warn("[售后退款] 分布式锁获取失败，降级 DB+渠道幂等兜底: paymentOrderId={}", paymentOrder.getId());
        }
        try {
            // 锁内累计上限复查（2026-09-30 审查修复：原在加锁前读，两笔并发售后可同时通过 → 超退）
            BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrder.getId());
            if (alreadyRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
                throw new CustomException("累计退款金额超过支付金额（已退：" + alreadyRefunded + "元，本次："
                        + refundAmount + "元）");
            }
            // 复用既有售后单退款号作渠道幂等键；registerNewProcessingRecord=false（同号新建会撞 uk_refund_no）
            ChannelOutcome outcome = invokeChannelRefund(paymentOrder, refundAmount, record.getReason(),
                    record.getRefundNo(), order.getId(), false, true);
            if (outcome.state == ChannelState.PROCESSING) {
                // 售后单审核通过后本地已是 processing，不落支付单/订单/库存/积分，终态以退款回调为准
                log.info("[售后退款] 渠道处理中，等待退款回调定终态: refundId={}, refundNo={}",
                        refundId, record.getRefundNo());
                return MSG_PROCESSING;
            }
            if (outcome.state == ChannelState.REJECTED) {
                throw new CustomException("退款渠道拒绝: " + outcome.errMsg);
            }
            if (outcome.state == ChannelState.EXCEPTION) {
                throw new CustomException("退款渠道调用失败，请稍后重试");
            }
            // 渠道同步成功：事务内本地落库（售后单本身 processing→SUCCESS + 全额联动）
            boolean isFull = alreadyRefunded.add(refundAmount).compareTo(paymentAmount) == 0;
            try {
                persistUserRefundTransactionally(record.getRefundNo(), paymentOrder, isFull);
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[售后退款] 本地落库失败: refundId={}, error={}", refundId, e.getMessage(), e);
                recordReconcileTraceSafely(paymentOrder, refundAmount,
                        "[对账待办]售后退款本地落库失败：" + e.getMessage());
                return "退款已提交（渠道已处理），请核对退款记录";
            }
            clearDashboardCache();
            log.info("[售后退款] 售后退款成功: refundId={}, orderId={}, amount={}", refundId, order.getId(), refundAmount);
            return MSG_SUCCESS;
        } finally {
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
        }
    }

    /**
     * 售后退款本地落库（事务内）：把既有售后单本身 processing→SUCCESS（{@code markUserRefundSuccess} 内含
     * 订单置已退款的 CAS + 白名单，与全额退款联动同口径）→ 全额时支付单 SUCCESS→REFUND（CAS）+ 会员权益/库存回退。
     * <p>收敛自 PaymentController.persistUserRefund，改为 REQUIRED 编程式事务，保证退款记录与全额联动原子
     *（原实现无事务包裹，markUserRefundSuccess 与支付单/权益/库存更新可部分失败）。</p>
     */
    private void persistUserRefundTransactionally(String refundNo, PaymentOrder paymentOrder, boolean isFull) {
        final Long poId = paymentOrder.getId();
        final Long orderId = paymentOrder.getOrderId();
        final Long poTenantId = paymentOrder.getTenantId();
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        txTemplate.execute(status -> {
            // 售后单本身即退款凭证（refund_type=1）：直接 processing→SUCCESS + 订单联动已退款(6)
            refundRecordService.markUserRefundSuccess(refundNo);
            if (isFull) {
                // 支付单 SUCCESS→REFUND（CAS，防覆盖非 SUCCESS 态）；订单已由 markUserRefundSuccess 联动，无需重复置位
                paymentOrderService.lambdaUpdate()
                        .eq(PaymentOrder::getId, poId)
                        .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                        .set(PaymentOrder::getStatus, STATUS_REFUND)
                        .set(PaymentOrder::getUpdateTime, LocalDateTime.now())
                        .update();
                // 全额售后退款：会员权益回退 + 菜品/原料库存回补（幂等，失败由补偿任务兜底）
                revertMemberRewardsAndStock(orderId, poTenantId);
            }
            return null;
        });
    }

    /**
     * 清除 Dashboard 概览缓存（退款成功后调用，确保概览数据实时准确）。收敛自 PaymentController。
     */
    private void clearDashboardCache() {
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            if (dashboardService != null && tenantId != null) {
                dashboardService.clearOverviewCache(tenantId);
            }
        } catch (RuntimeException e) {
            log.warn("清除Dashboard缓存失败", e);
        }
    }
}
