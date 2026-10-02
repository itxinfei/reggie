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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

    /**
     * 退款记录创建防重锁 key 前缀（后接 paymentOrderId）。
     * 与调用方 PaymentController / RefundServiceImpl 持有的 {@code payment:refund:lock:{id}} 刻意不同名，
     * 否则同一线程在外层锁内对本键 SETNX 会与自身冲突（分布式锁不可重入）。
     */
    private static final String REFUND_RECORD_LOCK_PREFIX = "payment:refund:record:lock:";

    /** 防重锁 TTL（秒）：覆盖锁内"重查已退总额 + 落库"耗时，与 RefundServiceImpl.LOCK_TTL_MS(30s) 同量级 */
    private static final long REFUND_RECORD_LOCK_TTL_SECONDS = 30L;

    /** tryLock 返回值哨兵：Redis 不可用（与 null="锁被占用" 区分，调用方据此走降级路径） */
    private static final String LOCK_REDIS_UNAVAILABLE = "REDIS_UNAVAILABLE";

    /** 释放锁脚本：比对锁值后才删除，防止误删他人重新持有的锁 */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    /**
     * 按 paymentOrderId 串行化退款创建请求的分布式锁（集群生效）。
     * 可选依赖：Redis 不可用时 fail-open，降级为调用方事务内的 DB 校验（见 {@link #createRefund}）。
     */
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

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
        // 集群防重（P0）：Redis SETNX 锁串行化同一支付单的退款创建，替换原 JVM 内 ConcurrentHashMap 锁
        //（多实例形同虚设 —— 原注释自认 fail-open，且锁 Map 只增不减）。
        // 锁内重查"已退总额"走一条原子聚合 SQL，不再依赖进程内计数。
        String lockKey = REFUND_RECORD_LOCK_PREFIX + paymentOrderId;
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            throw new CustomException("该退款单正在处理中，请勿重复提交");
        }
        if (LOCK_REDIS_UNAVAILABLE.equals(lockValue)) {
            // fail-open：Redis 不可用时降级为 DB 侧校验——调用方（PaymentController.refund /
            // RefundServiceImpl.persistRefundInTransaction）持 payment:refund:lock:{id} 分布式锁，
            // 并在事务内 SELECT ... FOR UPDATE 锁支付单行后复核累计退款额。
            log.warn("[退款] Redis 不可用，退款创建降级为事务内 DB 校验: {}", lockKey);
        }
        try {
            BigDecimal paid = paymentOrder.getAmount();
            if (paid == null || amount.compareTo(paid) > 0) {
                throw new CustomException("退款金额超过支付金额");
            }
            // 锁内重查该支付单累计已成功退款总额（单条 COALESCE(SUM(amount)) 聚合 SQL，跨租户由 Mapper
            // 的 @InterceptorIgnore 保证；支付单归属已在加锁前校验，此处无需再按 tenantId 过滤）。
            // 原 Java 侧 list + stream 求和在 currentTenantId 为 null（渠道回调场景）时会生成
            // tenant_id = NULL 条件而永远查不到已退记录，从而放行超额退款，一并修掉。
            BigDecimal refunded = sumRefundedAmount(paymentOrderId);
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
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 尝试获取退款记录创建防重锁（SETNX + TTL，锁值 UUID 用于释放时的 ownership 校验）。
     *
     * @param lockKey 锁 key
     * @return 锁值 UUID；{@link #LOCK_REDIS_UNAVAILABLE}=Redis 不可用（调用方降级）；null=锁被他人占用
     */
    private String tryLock(String lockKey) {
        if (stringRedisTemplate == null) {
            return LOCK_REDIS_UNAVAILABLE;
        }
        String lockValue = UUID.randomUUID().toString();
        try {
            Boolean success = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, REFUND_RECORD_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，Redis 故障不外抛，降级为 DB 校验
            log.error("[退款] 获取分布式锁异常，降级为 DB 校验: {}", lockKey, e);
            return LOCK_REDIS_UNAVAILABLE;
        }
    }

    /**
     * 释放分布式锁：Lua 脚本原子比对锁值后删除，防止误删他人锁；降级路径（哨兵值）不执行删除。
     */
    private void unlock(String lockKey, String lockValue) {
        if (stringRedisTemplate == null || lockValue == null || LOCK_REDIS_UNAVAILABLE.equals(lockValue)) {
            return;
        }
        try {
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(UNLOCK_LUA, Long.class),
                    Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            // 释放失败不影响业务结果，锁最迟在 TTL 到期后自动释放
            log.warn("[退款] 释放分布式锁失败，将由 TTL 兜底过期: {}, error={}", lockKey, e.getMessage(), e);
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
     * 处理 mark refund processing。
     * @param refundNo 退款单号
     * @return 是否 PENDING→PROCESSING
     */
    @Override
    public boolean markRefundProcessing(String refundNo) {
        // CAS：仅 PENDING 可转 PROCESSING；回调场景调用前已按记录 tenantId 设置 BaseContext
        return this.update(new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode())
                .set(RefundRecord::getStatus, RefundStatus.PROCESSING.getCode()));
    }

    /**
     * 处理 processing to success。
     * @param refundNo 退款单号
     * @return 是否首次 PROCESSING→SUCCESS
     */
    @Override
    public boolean markProcessingToSuccess(String refundNo) {
        // CAS：仅 PROCESSING 可转 SUCCESS，返回 false 即重复回调/非首次，调用方据此跳过联动
        return this.update(new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .eq(RefundRecord::getStatus, RefundStatus.PROCESSING.getCode())
                .set(RefundRecord::getStatus, RefundStatus.SUCCESS.getCode())
                .set(RefundRecord::getRefundTime, LocalDateTime.now()));
    }

    /**
     * 处理 processing to fail。
     * @param refundNo 退款单号
     * @param reason   失败原因
     * @return 是否 PROCESSING→FAIL
     */
    @Override
    public boolean markProcessingToFail(String refundNo, String reason) {
        // CAS：仅 PROCESSING 可转 FAIL；终态异常需人工核对渠道后台，故仅记录状态并告警
        boolean ok = this.update(new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .eq(RefundRecord::getStatus, RefundStatus.PROCESSING.getCode())
                .set(RefundRecord::getStatus, RefundStatus.FAIL.getCode()));
        if (ok) {
            log.warn("[退款] 处理中退款被渠道置为失败 refundNo={}, reason={}", refundNo, reason);
        }
        return ok;
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
     * 汇总区间内成功退款的金额与笔数（按创建时间）。
     */
    @Override
    public Map<String, Object> sumRefundBetween(Long tenantId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getStatus, RefundStatus.SUCCESS.getCode());
        if (tenantId != null) {
            qw.eq(RefundRecord::getTenantId, tenantId);
        }
        if (start != null) {
            qw.ge(RefundRecord::getCreatedTime, start);
        }
        if (end != null) {
            qw.le(RefundRecord::getCreatedTime, end);
        }
        BigDecimal amount = BigDecimal.ZERO;
        int count = 0;
        for (RefundRecord r : this.list(qw)) {
            if (r.getAmount() != null) {
                amount = amount.add(r.getAmount());
            }
            count++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("count", count);
        return result;
    }

    /**
     * 获取 refund analysis。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getRefundAnalysis(Long tenantId, String startDate, String endDate) {
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
        // 当前租户退款记录（含逻辑删除过滤，@TableLogic 自动生效）；
        // 传入日期时按 createdTime 区间过滤，不传则为累计口径
        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getTenantId, tenantId);
        if (StringUtils.isNotBlank(startDate)) {
            try {
                qw.ge(RefundRecord::getCreatedTime, LocalDate.parse(startDate.trim()).atStartOfDay());
            } catch (DateTimeParseException ex) {
                log.warn("[退款分析] startDate 格式非法已忽略: {}", startDate);
            }
        }
        if (StringUtils.isNotBlank(endDate)) {
            try {
                qw.le(RefundRecord::getCreatedTime, LocalDate.parse(endDate.trim()).atTime(23, 59, 59));
            } catch (DateTimeParseException ex) {
                log.warn("[退款分析] endDate 格式非法已忽略: {}", endDate);
            }
        }
        qw.orderByDesc(RefundRecord::getCreatedTime);
        List<RefundRecord> records = this.list(qw);

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
        // 回填支付单ID：refund_record.payment_order_id 为 NOT NULL，必须关联成功支付单，否则插入失败
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderId, orderId)
                .eq(PaymentOrder::getStatus, "SUCCESS")
                .last("LIMIT 1"));
        if (paymentOrder == null) {
            throw new CustomException("未找到该订单对应的成功支付单，无法申请售后");
        }
        record.setPaymentOrderId(paymentOrder.getId());
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

        // 1) 退款单 CAS 置成功：仅当当前状态仍是可推进态才落账，
        //    避免并发下覆盖其他路径（回调/自动退款）已写入的终态，也保证重复调用幂等
        String curRefundStatus = record.getStatus();
        if (RefundStatus.SUCCESS.getCode().equals(curRefundStatus)) {
            return; // 已是成功终态，幂等跳过
        }
        RefundRecord refundUpdate = new RefundRecord();
        refundUpdate.setId(record.getId());
        refundUpdate.setStatus(RefundStatus.SUCCESS.getCode());
        refundUpdate.setRefundTime(LocalDateTime.now());
        boolean refundOk = this.lambdaUpdate()
                .eq(RefundRecord::getId, record.getId())
                .eq(RefundRecord::getStatus, curRefundStatus)
                .update(refundUpdate);
        if (!refundOk) {
            log.warn("[售后退款] 退款单状态已被其他路径变更，跳过落账: refundNo={}", refundNo);
            return;
        }

        // 2) 订单置已退款：CAS + 状态白名单，与 RefundServiceImpl.updateOrderOnFullRefund 同口径。
        //    已取消(5) 允许流转——取消时库存/权益已回退，退款成功后应转 6，
        //    否则会留下"订单已取消 + 退款成功"的状态矛盾
        if (record.getOrderId() == null) {
            return;
        }
        try {
            Orders order = orderService.getById(record.getOrderId());
            if (order == null) {
                return;
            }
            Integer curOrderStatus = order.getStatus();
            if (Objects.equals(curOrderStatus, Orders.STATUS_REFUNDED)) {
                return; // 幂等
            }
            boolean allowed = Arrays.asList(
                    Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING,
                    Orders.STATUS_COMPLETED, Orders.STATUS_CANCELLED).contains(curOrderStatus);
            if (!allowed) {
                log.warn("[售后退款] 订单当前状态({})不允许置已退款，跳过: orderId={}",
                        curOrderStatus, record.getOrderId());
                return;
            }
            boolean orderOk = orderService.lambdaUpdate()
                    .eq(Orders::getId, record.getOrderId())
                    .eq(Orders::getStatus, curOrderStatus)
                    .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                    .update();
            if (!orderOk) {
                log.warn("[售后退款] 订单状态已变更，跳过标记: orderId={}, refundNo={}",
                        record.getOrderId(), refundNo);
                return;
            }
            log.info("[售后退款] 订单已标记退款: orderId={}, refundNo={}", record.getOrderId(), refundNo);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[售后退款] 标记订单退款失败: refundNo={}, error={}", refundNo, e.getMessage(), e);
        }
    }
}


