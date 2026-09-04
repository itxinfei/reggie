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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 分布式锁过期时间（毫秒）：覆盖一次渠道退款 HTTP 调用耗时 */
    private static final long LOCK_TTL_MS = 30 * 1000L; // 30秒

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

            // === 2.3 调用渠道退款（事务外，外部 HTTP 不被事务包裹） ===
            PaymentChannel paymentChannel = paymentChannelFactory.getChannel(latestCheck.getChannel());
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setChannelTradeNo(latestCheck.getChannelTradeNo());
            refundRequest.setAmount(refundAmount);
            refundRequest.setReason(reason);
            refundRequest.setOutRequestNo(refundNo);
            RefundResponse refundResponse;
            try {
                refundResponse = paymentChannel.refund(refundRequest);
            } catch (Exception e) {
                log.error("【严重】订单取消自动退款渠道调用异常，需人工处理！orderId={}, paymentOrderId={}, amount={}",
                        orderId, paymentOrderId, refundAmount, e);
                // 渠道调用异常（钱未出）——留对账待办痕迹，供 RefundReconcileTask 扫描告警人工退款，
                // 避免 campaign ENDED 后 scan 不再扫 OPEN 导致永久漏退（M1）
                recordReconcileTraceSafely(latestCheck, refundAmount, "[对账待办]渠道退款调用异常待人工");
                return false;
            }
            if (refundResponse == null || !refundResponse.isSuccess()) {
                String errMsg = refundResponse != null ? refundResponse.getErrorMsg() : "无响应";
                log.error("【严重】订单取消自动退款被渠道拒绝，需人工处理！orderId={}, paymentOrderId={}, errorMsg={}",
                        orderId, paymentOrderId, errMsg);
                // 渠道拒绝（钱未出）——留对账待办痕迹，供 RefundReconcileTask 扫描告警人工退款（M1）
                recordReconcileTraceSafely(latestCheck, refundAmount, "[对账待办]渠道退款被拒绝待人工：" + errMsg);
                return false;
            }

            // === 3. 事务内本地落库（行锁二次校验 + CAS 防覆盖） ===
            final BigDecimal fRefundAmount = refundAmount;
            final String fReason = (reason != null && !reason.trim().isEmpty()) ? reason : "订单自动退款";
            final Long fPaymentOrderId = paymentOrderId;
            try {
                new TransactionTemplate(transactionManager).execute(status -> {
                    // 重新查询支付单（防并发退款）
                    PaymentOrder latest = paymentOrderService.getById(fPaymentOrderId);
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
                    refundRecordService.recordReconcileTrace(fPaymentOrderId, fRefundAmount, fReason);
                } catch (Exception traceEx) {
                    log.error("【严重】渠道退款成功但本地落库失败，对账痕迹持久化也失败: orderId={}, paymentOrderId={}, refundAmount={}",
                            orderId, fPaymentOrderId, fRefundAmount, traceEx);
                }
                // 2. 主日志告警
                log.error("【严重】渠道退款成功但本地数据更新失败，需人工核对对账！orderId={}, paymentOrderId={}, refundAmount={}",
                        orderId, fPaymentOrderId, fRefundAmount, e);
                return false;
            }
            log.info("订单自动退款成功: orderId={}, paymentOrderId={}, refundAmount={}", orderId, fPaymentOrderId, fRefundAmount);
            return true;
        } finally {
            if (lockValue != null) {
                unlock(lockKey, lockValue);
            }
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
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
        } catch (Exception e) {
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
            log.error("【严重】渠道退款失败对账痕迹持久化也失败，需人工核查: paymentOrderId={}, amount={}",
                    paymentOrder.getId(), amount, traceEx);
        }
    }
}
