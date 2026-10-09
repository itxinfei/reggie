package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.payment.model.RefundRecord;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 退款记录服务接口
 * </p>
 * <p>管理退款申请及退款状态追踪</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface RefundRecordService extends IService<RefundRecord> {

    /**
     * 根据订单ID查询退款记录
     *
     * @param orderId 订单ID
     * @return 退款记录列表
     */
    List<RefundRecord> listByOrderId(Long orderId);

    /**
     * 创建退款记录
     *
     * @param paymentOrderId 支付订单ID
     * @param amount         退款金额
     * @param reason         退款原因
     * @return 退款记录
     */
    RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason);

    /**
     * 创建退款记录并复用指定退款单号。
     * <p>
     * 用于"先生成渠道幂等键 out_request_no、后本地落库"的退款流程：
     * 保证本地 refund_no 与渠道 out_request_no 一一对应，可直接对账。
     * refundNo 为空时回退内部生成。
     * </p>
     *
     * @param paymentOrderId 支付订单ID
     * @param amount         退款金额
     * @param reason         退款原因
     * @param refundNo       退款单号（渠道 out_request_no，可为 null 回退内部生成）
     * @return 退款记录
     */
    RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason, String refundNo);

    /**
     * 更新退款记录状态为成功（渠道退款同步成功后调用，修复原先记录永远停留在 PENDING 的问题）。
     *
     * @param refundNo 退款流水号
     */
    void markRefundSuccess(String refundNo);

    /**
     * 标记退款为处理中：PENDING → PROCESSING（渠道已受理但未定终态，如微信退款同步返回 PROCESSING）。
     * <p>CAS 更新，仅当当前状态为 PENDING 时生效；终态以退款异步回调为准。</p>
     *
     * @param refundNo 退款流水号
     * @return 是否迁移成功（记录不存在或状态非 PENDING 时返回 false）
     */
    boolean markRefundProcessing(String refundNo);

    /**
     * 处理中退款确认成功：PROCESSING → SUCCESS，并回填退款时间（退款异步回调 SUCCESS 时调用）。
     * <p>
     * CAS 更新，仅当当前状态为 PROCESSING 时生效。返回 true 表示<b>首次</b>确认成功，
     * 调用方据此执行一次性的全额联动（支付单/订单/库存/积分）；返回 false 表示该单此前
     * 已是终态（重复回调），必须幂等跳过联动。
     * </p>
     *
     * @param refundNo 退款流水号
     * @return 是否首次成功（PROCESSING→SUCCESS）；false 表示非首次或记录不存在
     */
    boolean markProcessingToSuccess(String refundNo);

    /**
     * 处理中退款确认失败：PROCESSING → FAIL（退款异步回调 CLOSED/ABNORMAL 等异常终态时调用），
     * 供人工核对渠道后台后处理。CAS 更新，仅 PROCESSING 可迁移。
     *
     * @param refundNo 退款流水号
     * @param reason   失败原因（渠道终态/错误描述）
     * @return 是否迁移成功
     */
    boolean markProcessingToFail(String refundNo, String reason);

    /**
     * 查询某支付单已成功退款的总金额（用于退款累计超额校验）。
     *
     * @param paymentOrderId 支付单ID
     * @return 已退款总额
     */
    BigDecimal sumRefundedAmount(Long paymentOrderId);

    /**
     * 退款分析（当前租户）：总数/成功/退款中/失败 + 成功退款总额 + 退款原因 TOP5
     *
     * @param tenantId 租户ID（为空取当前上下文）
     * @return 分析结果 Map
     */
    java.util.Map<String, Object> getRefundAnalysis(Long tenantId, String startDate, String endDate);

    /**
     * 汇总指定时间区间内成功退款的金额与笔数（按退款记录创建时间，状态 SUCCESS）。
     * <p>供日结使用：全额/部分退款均按记录实际金额统计，跨日订单的退款按退款发起日落在此区间。</p>
     *
     * @param tenantId 租户ID（为空不过滤租户）
     * @param start    起始时间（含，为空不限）
     * @param end      截止时间（含，为空不限）
     * @return Map：amount=成功退款合计(BigDecimal)，count=成功退款笔数(int)
     */
    java.util.Map<String, Object> sumRefundBetween(Long tenantId,
            java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 用户端发起售后申请（整单退款）。
     *
     * <p>校验：订单存在、归属当前用户、状态为已完成；同订单已有 PENDING 申请则拒绝重复申请。
     * 售后金额取订单实付金额（amount + deliveryFee），售后类型默认整单退款。</p>
     *
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 创建的退款记录（含 refundNo，状态 PENDING）
     */
    RefundRecord applyUserRefund(Long orderId, String reason);

    /**
     * 用户端查询某订单的售后申请记录。
     *
     * @param orderId 订单ID
     * @return 退款记录列表（按创建时间倒序）
     */
    List<RefundRecord> listUserRefundByOrderId(Long orderId);

    /**
     * 持久化"渠道已退款但本地落库失败"的对账待办痕迹。
     * <p>
     * 使用独立事务（REQUIRES_NEW），确保外层事务回滚后该痕迹仍存活，供对账定时任务扫描告警人工核对。
     * 痕迹以 {@code [对账待办]} 前缀的 reason 标识，状态为 PENDING。
     * </p>
     * <p>
     * <b>禁止</b>据此自动重试渠道退款——渠道侧可能已退款成功，重试会导致重复退款。
     * 仅作可观测痕迹，由人工核对渠道后台后决定是否标记 SUCCESS。
     * </p>
     *
     * @param paymentOrderId 支付单ID
     * @param amount         退款金额
     * @param reason         原始退款原因
     */
    void recordReconcileTrace(Long paymentOrderId, BigDecimal amount, String reason);

    /**
     * 查询待审核的售后申请列表（员工端）。
     *
     * @param status 售后状态（null=查全部，"pending"=待审核，"success"=已退款等）
     * @param tenantId 租户ID
     * @return 售后记录列表
     */
    List<RefundRecord> listUserRefunds(String status, Long tenantId);

    /**
     * 员工审核售后申请：通过/拒绝。
     * <p>
     * 通过（approve=true）：将售后单标记为 PROCESSING，由调用方触发渠道退款；
     * 拒绝（approve=false）：将售后单标记为 REJECTED，记录拒绝原因。
     * </p>
     *
     * @param refundId 售后记录ID
     * @param approve  是否通过
     * @param rejectReason 拒绝原因（拒绝时必填）
     * @return 更新后的售后记录
     */
    RefundRecord auditUserRefund(Long refundId, boolean approve, String rejectReason);

    /**
     * 标记售后退款为已退款成功（渠道退款成功后调用）。
     * 同时将关联订单状态置为已退款。
     *
     * @param refundNo 售后退款单号
     */
    void markUserRefundSuccess(String refundNo);
}
