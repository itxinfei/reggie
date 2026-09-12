package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.enums.RefundStatus;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.RefundRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 退款记录服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements
        RefundRecordService {

    /** 退款流水号时间格式 */
    private static final DateTimeFormatter REFUND_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 支付单Mapper（用于累计退款金额查询） */
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    /** 订单服务（@Lazy 避免与 OrderServiceImpl 的潜在循环依赖） */
    @Autowired
    @Lazy
    private OrderService orderService;

    /** 按 paymentOrderId 串行化退款创建请求，防止并发超额退款 */
    private final ConcurrentHashMap<Long, Object> refundLock = new ConcurrentHashMap<>();

    /**
     * 查询列表 by order id。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @Override
    public List<RefundRecord> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getPaymentOrderId, orderId)
                .eq(RefundRecord::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(RefundRecord::getCreatedTime));
    }

    /**
     * 创建 refund。
     * @param paymentOrderId 参数 paymentOrderId
     * @param amount 参数 amount
     * @param reason 参数 reason
     * @return 返回结果
     */
    @Override
    public RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason) {
        return createRefund(paymentOrderId, amount, reason, null);
    }

    /**
     * 创建 refund。
     * @param paymentOrderId 参数 paymentOrderId
     * @param amount 参数 amount
     * @param reason 参数 reason
     * @param refundNo 参数 refundNo
     * @return 返回结果
     */
    @Override
    public RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason, String refundNo) {
        if (amount == null) {
            throw new CustomException("退款金额不能为空");
        }
        // 租户归属校验：防止跨租户越权创建退款记录
        Long currentTenantId = BaseContext.getCurrentTenantId();
        PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentOrderId);
        if (paymentOrder == null) {
            throw new CustomException("支付单不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(paymentOrder.getTenantId())) {
            throw new CustomException("无权对其他租户的支付单发起退款");
        }
        // 串行化同一支付单的退款创建请求，防止并发超额退款（TOCTOU）
        Object lock = refundLock.computeIfAbsent(paymentOrderId, k -> new Object());
        synchronized (lock) {
            BigDecimal paid = paymentOrder.getAmount();
            if (paid == null || amount.compareTo(paid) > 0) {
                throw new CustomException("退款金额超过支付金额");
            }
            // 查询该支付单累计已退款金额（排除本条待创建记录）
            BigDecimal refunded = this.lambdaQuery()
                    .eq(RefundRecord::getPaymentOrderId, paymentOrderId)
                    .eq(RefundRecord::getTenantId, currentTenantId)
                    .eq(RefundRecord::getStatus, RefundStatus.SUCCESS.getCode())
                    .select(RefundRecord::getAmount)
                    .list()
                    .stream()
                    .map(RefundRecord::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (refunded.add(amount).compareTo(paid) > 0) {
                throw new CustomException("累计退款金额超过支付金额，当前已退款:"
                        + refunded + "，本次退款:" + amount);
            }

            RefundRecord record = new RefundRecord();
            record.setPaymentOrderId(paymentOrderId);
            record.setTenantId(currentTenantId);
            // 优先复用调用方传入的退款单号（即渠道 out_request_no），保证本地 refund_no 与渠道幂等键一一对应可对账；
            // 为空则内部生成。
            record.setRefundNo(refundNo != null && !refundNo.trim().isEmpty() ? refundNo : generateRefundNo());
            record.setAmount(amount);
            record.setReason(reason);
            record.setStatus(RefundStatus.PENDING.getCode());
            record.setCreatedTime(LocalDateTime.now());
            this.save(record);
            return record;
        }
    }

    /**
     * 处理 mark refund success。
     * @param refundNo 参数 refundNo
     */
    @Override
    public void markRefundSuccess(String refundNo) {
        // 租户归属校验：先查询再更新，防止跨租户越权标记退款成功
        RefundRecord record = lambdaQuery()
                .eq(RefundRecord::getRefundNo, refundNo)
                .one();
        if (record == null) {
            throw new CustomException("退款记录不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            throw new CustomException("无权操作其他租户的退款记录");
        }
        // 将退款记录从 PENDING 更新为 SUCCESS
        this.update(new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode())
                .set(RefundRecord::getStatus, RefundStatus.SUCCESS.getCode()));
    }

    /**
     * 处理 sum refunded amount。
     * @param paymentOrderId 参数 paymentOrderId
     * @return 返回结果
     */
    @Override
    public BigDecimal sumRefundedAmount(Long paymentOrderId) {
        // 查询该支付单已成功退款的总额（跨租户由 Mapper 的 @InterceptorIgnore 保证）
        BigDecimal sum = paymentOrderMapper.sumRefundedAmount(paymentOrderId, RefundStatus.SUCCESS.getCode());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * 获取 refund analysis。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getRefundAnalysis(Long tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (tenantId == null) tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            result.put("totalCount", 0);
            result.put("successCount", 0);
            result.put("pendingCount", 0);
            result.put("failCount", 0);
            result.put("totalAmount", BigDecimal.ZERO);
            result.put("byReason", new java.util.ArrayList<>());
            return result;
        }
        // 当前租户全部退款记录（含逻辑删除过滤，@TableLogic 自动生效）
        List<RefundRecord> records = this.list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getTenantId, tenantId)
                .orderByDesc(RefundRecord::getCreatedTime));

        int successCount = 0, pendingCount = 0, failCount = 0;
        BigDecimal successAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> reasonAmount = new LinkedHashMap<>();
        Map<String, Integer> reasonCount = new LinkedHashMap<>();
        for (RefundRecord r : records) {
            String st = r.getStatus();
            BigDecimal amt = r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO;
            if (RefundStatus.SUCCESS.getCode().equals(st)) {
                successCount++;
                successAmount = successAmount.add(amt);
                String reason = (r.getReason() == null || r.getReason().trim().isEmpty()) ? "其他" : r.getReason().trim();
                reasonAmount.merge(reason, amt, BigDecimal::add);
                reasonCount.merge(reason, 1, Integer::sum);
            } else if (RefundStatus.PENDING.getCode().equals(st)) {
                pendingCount++;
            } else {
                failCount++;
            }
        }
        result.put("totalCount", records.size());
        result.put("successCount", successCount);
        result.put("pendingCount", pendingCount);
        result.put("failCount", failCount);
        result.put("totalAmount", successAmount);

        // 退款原因 TOP5（按退款金额降序）
        List<Map<String, Object>> byReason = reasonAmount.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("reason", e.getKey());
                    m.put("amount", e.getValue());
                    m.put("count", reasonCount.getOrDefault(e.getKey(), 0));
                    return m;
                })
                .collect(Collectors.toList());
        result.put("byReason", byReason);
        return result;
    }

    /**
     * 生成退款流水号：RF + 时间戳 + UUID（保证唯一性）
     */
    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(REFUND_NO_FMT)
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 申请 user refund。
     * @param orderId 参数 orderId
     * @param reason 参数 reason
     * @return 返回结果
     */
    @Override
    public RefundRecord applyUserRefund(Long orderId, String reason) {
        if (orderId == null) {
            throw new CustomException("订单ID不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new CustomException("退款原因不能为空");
        }
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentUserId == null || currentTenantId == null) {
            throw new CustomException("请先登录");
        }
        // 1. 订单存在性 + 归属校验（同租户 + 同用户）
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (!currentTenantId.equals(order.getTenantId()) || !currentUserId.equals(order.getUserId())) {
            throw new CustomException("无权对此订单申请售后");
        }
        // 2. 状态校验：仅已完成订单可申请售后（防止已取消/已退款重复申请）
        if (!Objects.equals(order.getStatus(), Orders.STATUS_COMPLETED)) {
            throw new CustomException("仅已完成订单可申请售后，其他状态请联系客服");
        }
        // 3. 重复申请校验：同订单已有 PENDING 售后申请则拒绝
        long pendingCount = this.count(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId)
                .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode()));
        if (pendingCount > 0) {
            throw new CustomException("该订单已有售后申请处理中，请勿重复申请");
        }
        // 4. 计算售后金额：订单实付（amount 已包含菜品 + 配送费 + 满减/折扣后的实付金额，不应再加 deliveryFee）
        BigDecimal paidAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("订单实付金额为0，无法申请售后");
        }
        // 5. 创建售后记录
        RefundRecord record = new RefundRecord();
        record.setOrderId(orderId);
        record.setTenantId(currentTenantId);
        record.setRefundNo(generateRefundNo());
        record.setAmount(paidAmount);
        record.setReason(reason.trim());
        record.setStatus(RefundStatus.PENDING.getCode());
        record.setRefundType(1); // 1=整单退款
        record.setApplyUserId(currentUserId);
        record.setCreatedTime(LocalDateTime.now());
        this.save(record);
        return record;
    }

    /**
     * 查询列表 user refund by order id。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @Override
    public List<RefundRecord> listUserRefundByOrderId(Long orderId) {
        if (orderId == null) {
            return java.util.Collections.emptyList();
        }
        Long currentUserId = BaseContext.getCurrentId();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        // 用户端仅能查自己的订单售后记录
        Orders order = orderService.getById(orderId);
        if (order == null || currentTenantId != null && !currentTenantId.equals(order.getTenantId())
                || currentUserId != null && !currentUserId.equals(order.getUserId())) {
            throw new CustomException("无权查询此订单的售后记录");
        }
        return this.list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId)
                .orderByDesc(RefundRecord::getCreatedTime));
    }

    /**
     * 持久化"渠道已退款但本地落库失败"的对账待办痕迹。
     * <p>
     * 独立事务（REQUIRES_NEW）写入 PENDING 痕迹，reason 以 {@code [对账待办]} 前缀标识，
     * 供 {@link com.reggie.module.payment.task.RefundReconcileTask} 扫描告警人工核对。
     * tenantId 直接取自支付单，不依赖 BaseContext（调用方可能已无租户上下文）。
     * </p>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordReconcileTrace(Long paymentOrderId, BigDecimal amount, String reason) {
        PaymentOrder paymentOrder = paymentOrderMapper.selectById(paymentOrderId);
        if (paymentOrder == null) {
            log.warn("[对账待办] 支付单不存在，跳过痕迹持久化: paymentOrderId={}", paymentOrderId);
            return;
        }
        String safeReason = (reason != null && !reason.trim().isEmpty())
                ? reason : "渠道已退款本地落库失败";
        RefundRecord trace = new RefundRecord();
        trace.setPaymentOrderId(paymentOrderId);
        trace.setOrderId(paymentOrder.getOrderId());
        trace.setTenantId(paymentOrder.getTenantId());
        trace.setRefundNo(generateRefundNo());
        trace.setAmount(amount);
        trace.setReason("[对账待办]" + safeReason);
        trace.setStatus(RefundStatus.PENDING.getCode());
        trace.setCreatedTime(LocalDateTime.now());
        this.save(trace);
        log.warn("[对账待办] 已持久化渠道退款成功但本地落库失败的待办痕迹: paymentOrderId={}, amount={}, refundNo={}",
                paymentOrderId, amount, trace.getRefundNo());
    }

    // ==================== 用户售后审核 ====================

    /**
     * 查询列表 user refunds。
     * @param status 参数 status
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<RefundRecord> listUserRefunds(String status, Long tenantId) {
        Long effectiveTenantId = tenantId != null ? tenantId : BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<>();
        if (effectiveTenantId != null) {
            qw.eq(RefundRecord::getTenantId, effectiveTenantId);
        }
        if (StringUtils.isNotBlank(status)) {
            qw.eq(RefundRecord::getStatus, status);
        }
        // 售后单：reason 不以 [对账待办] 开头的才是真实售后单（[对账待办] 为对账痕迹）
        qw.notLike(RefundRecord::getReason, "[对账待办]")
           .orderByDesc(RefundRecord::getCreatedTime);
        return this.list(qw);
    }

    /**
     * 审核 user refund。
     * @param refundId 参数 refundId
     * @param approve 参数 approve
     * @param rejectReason 参数 rejectReason
     * @return 返回结果
     */
    @Override
    public RefundRecord auditUserRefund(Long refundId, boolean approve, String rejectReason) {
        if (refundId == null) {
            throw new CustomException("售后记录ID不能为空");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        RefundRecord record = this.getById(refundId);
        if (record == null) {
            throw new CustomException("售后记录不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(record.getTenantId())) {
            throw new CustomException("无权操作其他租户的售后记录");
        }
        if (!Objects.equals(record.getStatus(), RefundStatus.PENDING.getCode())) {
            throw new CustomException("该售后记录已处理，不可重复审核");
        }
        if (approve) {
            // 审核通过 → 标记 PROCESSING，实际退款由 PaymentController.refund 或定时任务执行
            record.setStatus(RefundStatus.PROCESSING.getCode());
            record.setAuditUserId(BaseContext.getCurrentId());
            record.setAuditTime(LocalDateTime.now());
            this.updateById(record);
            log.info("[售后审核] 审核通过: refundId={}, refundNo={}, orderId={}, amount={}",
                    refundId, record.getRefundNo(), record.getOrderId(), record.getAmount());
        } else {
            // 审核拒绝 → 标记 REJECTED
            if (StringUtils.isBlank(rejectReason)) {
                throw new CustomException("拒绝时必须填写拒绝原因");
            }
            record.setStatus(RefundStatus.REJECTED.getCode());
            record.setAuditUserId(BaseContext.getCurrentId());
            record.setAuditTime(LocalDateTime.now());
            record.setRejectReason(rejectReason.trim());
            this.updateById(record);
            log.info("[售后审核] 审核拒绝: refundId={}, refundNo={}, reason={}",
                    refundId, record.getRefundNo(), rejectReason);
        }
        return record;
    }

    /**
     * 处理 mark user refund success。
     * @param refundNo 参数 refundNo
     */
    @Override
    public void markUserRefundSuccess(String refundNo) {
        if (StringUtils.isBlank(refundNo)) {
            return;
        }
        RefundRecord record = this.getOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .last("LIMIT 1"));
        if (record == null) {
            log.warn("[售后退款] 售后记录不存在: refundNo={}", refundNo);
            return;
        }
        record.setStatus(RefundStatus.SUCCESS.getCode());
        record.setRefundTime(LocalDateTime.now());
        this.updateById(record);
        // 将关联订单状态置为已退款
        if (record.getOrderId() != null) {
            try {
                Orders order = orderService.getById(record.getOrderId());
                if (order != null && !Objects.equals(order.getStatus(), Orders.STATUS_REFUNDED)) {
                    orderService.lambdaUpdate()
                            .eq(Orders::getId, order.getId())
                            .eq(Orders::getTenantId, record.getTenantId())
                            .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                            .update();
                    log.info("[售后退款] 订单已标记退款: orderId={}, refundNo={}", order.getId(), refundNo);
                }
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[售后退款] 标记订单退款失败: refundNo={}, error={}", refundNo, e.getMessage(), e);
            }
        }
    }
}


