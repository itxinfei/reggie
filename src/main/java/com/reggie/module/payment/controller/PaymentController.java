package com.reggie.module.payment.controller;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.dto.PayRequestDTO;
import com.reggie.dto.RefundRequestDTO;
import com.reggie.module.order.model.Orders;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.channel.PayRequest;
import com.reggie.module.payment.channel.PayResponse;
import com.reggie.module.payment.channel.RefundRequest;
import com.reggie.module.payment.channel.RefundResponse;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.model.RefundRecord;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_REFUND;
import static com.reggie.module.payment.model.PaymentOrder.STATUS_SUCCESS;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundRecordService;
import com.reggie.module.payment.service.RefundService;
import com.reggie.enums.RefundStatus;
import com.reggie.module.dashboard.service.DashboardService;
import com.reggie.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 聚合支付控制器
 * 提供统一支付、退款、回调处理等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
@Tag(name = "聚合支付", description = "统一支付、退款、支付回调等接口")
public class PaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundRecordService refundRecordService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @Autowired
    private RefundService refundService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private com.reggie.module.member.service.MemberRewardService memberRewardService;

    /** 全额退款时同步回补菜品+原料库存（幂等） */
    @Autowired
    private com.reggie.module.order.service.OrderStockRefundService orderStockRefundService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.reggie.module.payment.mapper.PaymentOrderMapper paymentOrderMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 退款分布式锁过期时间（毫秒）：覆盖一次渠道退款 HTTP 调用耗时 */
    private static final long REFUND_LOCK_TTL_MS = 30 * 1000L; // 30秒

    /**
     * 创建支付订单
     * @param dto 支付请求参数
     * @return 支付渠道响应（支付链接或二维码）
     */
    @PostMapping("/pay")
    @RateLimit(maxRequestsPerSecond = 3, type = RateLimitType.USER)
    @Operation(summary = "创建支付订单", description = "创建支付订单并调用支付渠道生成支付链接或二维码")
    public R<PayResponse> pay(
            @Parameter(description = "支付请求参数", required = true) @Validated @RequestBody PayRequestDTO dto) {
        // 金额从数据库订单读取，禁止使用客户端传入金额（防篡改）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            return R.error("租户信息缺失");
        }
        Orders order = orderService.getById(dto.getOrderId());
        if (order == null) {
            return R.error("订单不存在");
        }
        if (!currentTenantId.equals(order.getTenantId())) {
            return R.error("无权操作该订单");
        }
        // 校验订单为待付款状态
        if (!Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
            return R.error("订单状态不允许支付");
        }
        // 防御性 null 检查：order.amount 可能在数据库中为 null（历史数据或绕过校验）
        BigDecimal payAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 0 元订单：优惠券/折扣将实付压到 0，自动完成支付，不调用渠道
            paymentOrderService.lambdaUpdate()
                    .eq(PaymentOrder::getOrderId, dto.getOrderId())
                    .eq(PaymentOrder::getStatus, "PENDING")
                    .set(PaymentOrder::getStatus, "SUCCESS")
                    .set(PaymentOrder::getPaidTime, LocalDateTime.now())
                    .update();
            orderService.lambdaUpdate()
                    .eq(Orders::getId, dto.getOrderId())
                    .eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                    .set(Orders::getStatus, Orders.STATUS_ORDERED)
                    .set(Orders::getCheckoutTime, LocalDateTime.now())
                    .update();
            log.info("0元订单自动完成支付: orderId={}", dto.getOrderId());
            // 修复：方法返回类型为 R<PayResponse>，0 元分支须返回 PayResponse（成功标记），不能返回 String
            PayResponse zeroPayResponse = new PayResponse();
            zeroPayResponse.setSuccess(true);
            zeroPayResponse.setRawResponse("0元订单自动完成支付");
            return R.success(zeroPayResponse);
        }

        PaymentOrder paymentOrder = paymentOrderService.createPaymentOrder(dto.getOrderId(), dto.getChannel(),
                payAmount);

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(dto.getChannel());
        PayRequest request = new PayRequest();
        request.setTradeNo(paymentOrder.getTradeNo());
        request.setAmount(payAmount);
        request.setSubject("瑞吉外卖-订单" + dto.getOrderId());
        PayResponse response = paymentChannel.createOrder(request);
        // 回填商户支付单号：沙箱(mock-mode)收银台需用它作为 out_trade_no 发起模拟支付回调
        response.setTradeNo(paymentOrder.getTradeNo());

        return R.success(response);
    }

    /**
     * 接收支付渠道的异步通知
     * @param channel 支付渠道：WECHAT-微信、ALIPAY-支付宝
     * @param params 回调参数
     * @return 处理结果
     */
    @PostMapping("/notify/{channel}")
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.IP)
    @Operation(summary = "支付回调通知", description = "接收支付渠道的异步通知，更新订单支付状态")
    public R<String> notify(
                        @Parameter(description = "支付渠道：WECHAT-微信、ALIPAY-支付宝", required =
                                true) @PathVariable String channel,
            @Parameter(description = "回调参数") @RequestBody Map<String, String> params) {
        // 回调场景用 getChannelNullable：未知/空渠道返回 200 + 业务错误码（而非抛异常触发 500），
        // 主动停止支付平台重试（平台对 5xx 会重试，对 2xx 业务失败码通常不重试）
        PaymentChannel paymentChannel = paymentChannelFactory.getChannelNullable(channel);
        if (paymentChannel == null) {
            log.warn("支付回调渠道不支持，拒绝处理：channel={}", channel);
            return R.error("不支持的支付通道");
        }
        // 签名校验：禁止直接信任未验签的回调参数（防回调伪造）
        if (!paymentChannel.verifyNotifySign(params)) {
            log.warn("支付回调签名校验失败：channel={}, params={}", channel, params);
            return R.error("回调签名校验失败");
        }
        String tradeNo = params.get("out_trade_no");
        if (tradeNo == null || tradeNo.trim().isEmpty()) {
            log.warn("支付回调缺少 out_trade_no 参数，channel={}", channel);
            return R.error("回调缺少 out_trade_no 参数");
        }
        // 防御性校验：tradeNo 必须对应真实存在的支付单，避免伪造不存在的交易号
        PaymentOrder exist = paymentOrderService.selectByTradeNoIgnoreTenant(tradeNo);
        if (exist == null) {
            log.warn("支付回调 tradeNo 不存在，拒绝处理：channel={}, tradeNo={}", channel, tradeNo);
            return R.error("回调交易号不存在");
        }
        // 修复 P1：渠道一致性校验 — 防止用 WECHAT 回调参数（含有效签名）伪造 ALIPAY 支付单
        if (!channel.equals(exist.getChannel())) {
            log.warn("支付回调渠道不一致，拒绝处理：pathChannel={}, orderChannel={}, tradeNo={}",
                    channel, exist.getChannel(), tradeNo);
            return R.error("支付渠道不匹配");
        }
        // 修复 P2：金额一致性校验 — 回调金额与支付单金额比对，防回调金额篡改
        // P1 修复：按渠道读取金额字段，微信回调为 total_fee（分），支付宝回调为 total_amount（元）。
        // 此前只读 total_fee，支付宝回调（无 total_fee 字段）金额校验被静默跳过，等于金额防线失效。
        String channelFeeField = "WECHAT".equalsIgnoreCase(channel) ? "total_fee" : "total_amount";
        String notifyAmountStr = params.get(channelFeeField);
        if (notifyAmountStr != null && !notifyAmountStr.trim().isEmpty()) {
            try {
                // total_fee 单位为分（微信），total_amount 单位为元（支付宝），统一换算为元比对
                BigDecimal notifyAmount;
                if ("WECHAT".equalsIgnoreCase(channel)) {
                    notifyAmount = new BigDecimal(notifyAmountStr).divide(new BigDecimal("100"), 2,
                            java.math.RoundingMode.HALF_UP);
                } else {
                    notifyAmount = new BigDecimal(notifyAmountStr);
                }
                BigDecimal existAmount = exist.getAmount();
                // 金额校验必须 fail-closed：支付单金额缺失属数据异常，拒绝而非放行
                // （若在此短路跳过，攻击者可用任意回调金额通过校验篡改支付结果）
                if (existAmount == null) {
                    log.warn("支付单金额缺失，拒绝处理：tradeNo={}", tradeNo);
                    return R.error("支付金额非法");
                }
                if (notifyAmount.compareTo(existAmount) != 0) {
                    log.warn("支付回调金额不一致，拒绝处理：notifyAmount={}, orderAmount={}, tradeNo={}",
                            notifyAmount, existAmount, tradeNo);
                    return R.error("支付金额不一致");
                }
            } catch (NumberFormatException e) {
                log.warn("支付回调 {} 格式非法，拒绝处理：value={}, tradeNo={}",
                        channelFeeField, notifyAmountStr, tradeNo);
                return R.error("支付金额格式非法");
            }
        }
        PayResponse response = paymentChannel.handleNotify(params);
        if (response.isSuccess()) {
            paymentOrderService.handlePaymentSuccess(tradeNo, response.getChannelTradeNo());
            return R.success("回调处理成功");
        }
        log.warn("支付回调处理失败：channel={}, errorMsg={}", channel, response.getErrorMsg());
        return R.error("回调处理失败");
    }

    /**
     * 退款分析（当前租户）
     * <p>返回退款总数/成功/退款中/失败、成功退款总额、退款原因 TOP5，供报表页退款分析。</p>
     *
     * @return 退款分析结果
     */
    @RequireEmployee
    @GetMapping("/refund/stats")
    @Operation(summary = "退款分析", description = "当前租户退款统计与退款原因TOP5")
    public R<Map<String, Object>> refundStats() {
        return R.success(refundRecordService.getRefundAnalysis(BaseContext.getCurrentTenantId()));
    }

    /**
     * 查询待审核/已审核的售后申请列表（员工端）。
     *
     * @param status   售后状态筛选（pending/processing/success/rejected/fail，null=全部）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 售后记录分页列表
     */
    @RequireEmployee
    @GetMapping("/refund/user/list")
    @Operation(summary = "售后申请列表", description = "查询用户的售后退款申请，支持按状态筛选")
    public R<Page<RefundRecord>> userRefundList(
            @Parameter(description = "售后状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize) {
        Page<RefundRecord> pageInfo = PageUtils.of(page, pageSize);
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(RefundRecord::getTenantId, tenantId);
        }
        // 过滤掉对账痕迹（reason 以 [对账待办] 开头），只查真实售后单
        qw.notLike(RefundRecord::getReason, "[对账待办]");
        if (status != null && !status.isEmpty()) {
            qw.eq(RefundRecord::getStatus, status);
        }
        qw.orderByDesc(RefundRecord::getCreatedTime);
        return R.success(refundRecordService.page(pageInfo, qw));
    }

    /**
     * 员工审核售后申请（通过/拒绝）。
     * <p>
     * 审核通过后标记为 PROCESSING，需再调用 {@code /payment/refund/user/execute} 触发渠道退款；
     * 拒绝后标记为 REJECTED，记录拒绝原因，用户可重新申请。
     * </p>
     *
     * @param refundId     售后记录ID
     * @param approve      true=通过, false=拒绝
     * @param rejectReason 拒绝原因（拒绝时必填）
     * @return 审核结果
     */
    @RequireEmployee
    @PostMapping("/refund/user/audit")
    @Operation(summary = "审核售后申请", description = "审核通过/拒绝用户的售后退款申请")
    public R<String> auditUserRefund(
            @Parameter(description = "售后记录ID", required = true) @RequestParam Long refundId,
            @Parameter(description = "是否通过", required = true) @RequestParam boolean approve,
            @Parameter(description = "拒绝原因") @RequestParam(required = false) String rejectReason) {
        try {
            refundRecordService.auditUserRefund(refundId, approve, rejectReason);
            return R.success(approve ? "审核通过" : "已拒绝");
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 执行售后退款（审核通过后触发渠道退款）。
     * <p>
     * 从售后记录中查找关联支付单，复用退款流程（Redis 锁 + 渠道退款 + 本地落库）。
     * 售后单必须处于 PROCESSING 状态（审核已通过）。
     * </p>
     *
     * @param refundId 售后记录ID
     * @return 退款结果
     */
    @RequireEmployee
    @PostMapping("/refund/user/execute")
    @Operation(summary = "执行售后退款", description = "审核通过后触发渠道退款，完成售后闭环")
    public R<String> executeUserRefund(
            @Parameter(description = "售后记录ID", required = true) @RequestParam Long refundId) {
        RefundRecord record = refundRecordService.getById(refundId);
        if (record == null) {
            return R.error("售后记录不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(record.getTenantId())) {
            return R.error("无权操作其他租户的售后记录");
        }
        if ("SUCCESS".equals(record.getStatus())) {
            return R.success("该售后单已退款成功，请勿重复操作");
        }
        if (!"processing".equals(record.getStatus())) {
            return R.error("该售后单当前状态不支持退款执行（需先审核通过）");
        }
        // 查找关联订单对应的支付单
        Orders order = orderService.getById(record.getOrderId());
        if (order == null) {
            return R.error("关联订单不存在");
        }
        // 查找该订单的已成功支付单
        PaymentOrder paymentOrder = paymentOrderService.lambdaQuery()
                .eq(PaymentOrder::getOrderId, order.getId())
                .eq(PaymentOrder::getTenantId, tenantId)
                .eq(PaymentOrder::getStatus, "SUCCESS")
                .orderByDesc(PaymentOrder::getPaidTime)
                .last("LIMIT 1")
                .one();
        if (paymentOrder == null) {
            return R.error("未找到该订单的有效支付单，无法退款");
        }
        BigDecimal refundAmount = record.getAmount();
        BigDecimal paymentAmount = paymentOrder.getAmount();
        if (paymentAmount == null) {
            return R.error("支付金额异常，无法退款");
        }
        BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrder.getId());
        if (alreadyRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
            return R.error("累计退款金额超过支付金额（已退：" + alreadyRefunded + "元，本次：" + refundAmount + "元）");
        }

        // 复用退款流程的核心部分：Redis 锁 → 渠道退款 → 本地落库
        String refundLockKey = "payment:refund:lock:" + paymentOrder.getId();
        String refundLockValue = tryRefundLock(refundLockKey);
        try {
            R<String> channelResult = doChannelRefund(paymentOrder, record, refundAmount, refundId);
            if (channelResult != null) {
                return channelResult;
            }
            R<String> persistResult = persistUserRefund(paymentOrder, order, record, refundAmount,
                    alreadyRefunded, paymentAmount);
            if (persistResult != null) {
                return persistResult;
            }
            clearDashboardCache();
            log.info("[售后退款] 售后退款成功: refundId={}, orderId={}, amount={}", refundId, order.getId(), refundAmount);
            return R.success("退款成功");
        } finally {
            if (refundLockValue != null) {
                unlockRefundLock(refundLockKey, refundLockValue);
            }
        }
    }

    /**
     * 调用渠道退款，成功返回 null（继续落库），失败返回错误响应（等价抽取）。
     *
     * @return 失败时的响应或 null
     */
    private R<String> doChannelRefund(PaymentOrder paymentOrder, RefundRecord record, BigDecimal refundAmount,
            Long refundId) {
        PaymentChannel channel = paymentChannelFactory.getChannel(paymentOrder.getChannel());
        if (channel == null) {
            return R.error("不支持的支付渠道: " + paymentOrder.getChannel());
        }
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        refundRequest.setAmount(refundAmount);
        refundRequest.setReason(record.getReason());
        refundRequest.setOutRequestNo(record.getRefundNo());
        RefundResponse refundResponse;
        try {
            refundResponse = channel.refund(refundRequest);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[售后退款] 渠道调用异常: refundId={}, error={}", refundId, e.getMessage(), e);
            recordReconcileTraceSafely(paymentOrder.getId(), refundAmount,
                    "[对账待办]售后退款渠道调用异常：" + e.getMessage());
            return R.error("退款渠道调用失败，请稍后重试");
        }
        if (refundResponse == null || !refundResponse.isSuccess()) {
            String errMsg = refundResponse != null ? refundResponse.getErrorMsg() : "无响应";
            log.warn("[售后退款] 渠道退款被拒绝: refundId={}, error={}", refundId, errMsg);
            recordReconcileTraceSafely(paymentOrder.getId(), refundAmount,
                    "[对账待办]售后退款渠道被拒绝：" + errMsg);
            return R.error("退款渠道拒绝: " + errMsg);
        }
        return null;
    }

    /**
     * 渠道退款成功后的本地落库，成功返回 null（继续），失败返回提示响应（等价抽取）。
     *
     * @return 失败时的响应或 null
     */
    private R<String> persistUserRefund(PaymentOrder paymentOrder, Orders order, RefundRecord record,
            BigDecimal refundAmount, BigDecimal alreadyRefunded, BigDecimal paymentAmount) {
        try {
            refundRecordService.createRefund(paymentOrder.getId(), refundAmount,
                    "[售后退款]" + record.getReason(), record.getRefundNo());
            refundRecordService.markRefundSuccess(record.getRefundNo());
            refundRecordService.markUserRefundSuccess(record.getRefundNo());
            boolean isFull = alreadyRefunded.add(refundAmount).compareTo(paymentAmount) == 0;
            if (isFull) {
                paymentOrderService.lambdaUpdate()
                        .eq(PaymentOrder::getId, paymentOrder.getId())
                        .set(PaymentOrder::getStatus, "REFUND")
                        .update();
                orderService.lambdaUpdate()
                        .eq(Orders::getId, order.getId())
                        .set(Orders::getStatus, Orders.STATUS_REFUNDED)
                        .update();
                // 全额售后退款：同步回补菜品+原料库存（失败仅日志，补偿任务兜底）
                try {
                    orderStockRefundService.restoreForOrder(order.getId());
                } catch (Exception ex) {
                    log.error("[售后退款] 库存回补异常，待补偿任务兜底: orderId={}", order.getId(), ex);
                }
                // 该路径原本未回退会员权益，补齐积分回退 + 优惠券恢复
                try {
                    memberRewardService.reverseRewards(order.getId(), paymentOrder.getTenantId());
                } catch (Exception ex) {
                    log.error("[售后退款] 会员权益回退失败，需人工核查: orderId={}", order.getId(), ex);
                }
            }
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[售后退款] 本地落库失败: refundId={}, error={}", record.getId(), e.getMessage(), e);
            recordReconcileTraceSafely(paymentOrder.getId(), refundAmount,
                    "[对账待办]售后退款本地落库失败：" + e.getMessage());
            return R.success("退款已提交（渠道已处理），请核对退款记录");
        }
        return null;
    }

    /**
     * 申请退款
     * <p>
     * 退款流程：①校验（无事务）→ ②调用渠道退款（事务外，外部 HTTP 不被事务包裹）→ ③本地落库（短事务）。
     * 拆分事务避免渠道 HTTP 调用期间长事务持有 DB 锁；渠道成功但本地失败需人工对账。
     * </p>
     * @param dto 退款请求参数
     * @return 退款结果
     */
    @RequireEmployee
    @PostMapping("/refund")
    @Operation(summary = "申请退款", description = "申请退款并调用支付渠道处理退款流程")
    public R<String> refund(
            @Parameter(description = "退款请求参数", required = true) @Validated @RequestBody RefundRequestDTO dto) {
        // === 1. 校验阶段（无事务，避免长事务持有 DB 锁） ===
        PaymentOrder paymentOrder = paymentOrderService.getById(dto.getPaymentOrderId());
        if (paymentOrder == null) {
            return R.error("支付订单不存在");
        }
        BigDecimal refundAmount = dto.getAmount();
        BigDecimal paymentAmount = paymentOrder.getAmount();
        // 校验租户归属/状态机/金额上限（等价抽取，降低方法长度）
        R<String> validationError = validateRefundRequest(dto, paymentOrder, refundAmount, paymentAmount);
        if (validationError != null) {
            return validationError;
        }

        // 离线支付通道（现金/银行卡/储值/货到付款）退款由人工完成，系统仅做本地记账闭环，不调渠道 API。
        // 避免 paymentChannelFactory.getChannel 抛“不支持的支付通道”导致员工手动退款 500（Defect B）。
        if (!refundService.isOnlineChannel(paymentOrder.getChannel())) {
            boolean offlineOk = refundService.refundOfflineByPaymentOrderId(paymentOrder.getId(), refundAmount,
                    dto.getReason());
            if (!offlineOk) {
                return R.error("线下支付退款记账失败，需人工核查");
            }
            clearDashboardCache();
            log.info("[退款] 线下支付本地记账退款成功: paymentOrderId={}, channel={}, amount={}",
                    paymentOrder.getId(), paymentOrder.getChannel(), refundAmount);
            return R.success("退款成功（线下支付，已记录手动退款）");
        }

        // === 1.5 Redis 分布式锁串行化同一支付单的退款发起 ===
        // P0 修复（双重扣款根因）：此前"校验（阶段1）→ 渠道调用（阶段2）→ FOR UPDATE 落库（阶段3）"三段式中，
        // 渠道调用发生在 FOR UPDATE 行锁之前。两个并发退款都通过阶段1校验后双双调用渠道，
        // 渠道重复扣款后本地 FOR UPDATE 只能拦住第二个的落库——资金已出、记录未建，形成双重扣款。
        // 现于"校验后、渠道调用前"加 Redis 锁 payment:refund:lock:{paymentOrderId}，锁内串行化：
        // 重新查询支付单状态（非 SUCCESS 拒绝）+ 生成 outRequestNo（渠道幂等键）+ 渠道调用，finally 释放锁。
        // fail-open：Redis 不可用时降级到 DB FOR UPDATE + 渠道 out_request_no 幂等兜底（RefundRecordServiceImpl
        // 单实例 ConcurrentHashMap 仍有保护）。
        final Long fPaymentOrderId = paymentOrder.getId();
        String refundLockKey = "payment:refund:lock:" + fPaymentOrderId;
        String refundLockValue = tryRefundLock(refundLockKey);
        if (refundLockValue == null) {
            log.warn("退款分布式锁获取失败，降级 DB+渠道幂等兜底: paymentOrderId={}", fPaymentOrderId);
        }
        try {
            // === 1.6 锁内重查支付单（防止锁前已退款/状态已变更，非 SUCCESS 拒绝） ===
            PaymentOrder lockedOrder = paymentOrderService.getById(fPaymentOrderId);
            if (lockedOrder == null || !STATUS_SUCCESS.equals(lockedOrder.getStatus())) {
                log.warn("退款重查支付单状态已变更，拒绝退款：paymentOrderId={}, status={}",
                        fPaymentOrderId, lockedOrder != null ? lockedOrder.getStatus() : "null");
                return R.error("支付订单状态已变更，请刷新后重试");
            }
            BigDecimal lockedRefunded = refundRecordService.sumRefundedAmount(fPaymentOrderId);
            if (lockedRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
                return R.error("累计退款金额超过支付金额（已退：" + lockedRefunded + "元）");
            }

            // === 1.7 生成退款单号并作为渠道幂等键（out_request_no） ===
            // 微信/支付宝以同一商户退款单号做退款幂等去重：重复请求只退款一次，
            // 防"本地落库失败后重试/并发退款"造成的双重扣款。
            final String refundNo = generateRefundNo();

            // === 2. 调用渠道退款（等价抽取，事务外，外部 HTTP 不应被事务包裹） ===
            R<String> channelError = callChannelRefund(lockedOrder, refundAmount, dto.getReason(), refundNo,
                    fPaymentOrderId);
            if (channelError != null) {
                return channelError;
            }

            // === 3. 事务内更新本地数据（等价抽取，渠道已退款成功，本地必须落库） ===
            R<String> persistError = persistRefundTransactionally(fPaymentOrderId, refundAmount, dto.getReason(),
                    refundNo);
            if (persistError != null) {
                return persistError;
            }

            // 退款成功后清除 Dashboard 缓存，确保今日订单/营业额数据实时准确
            clearDashboardCache();
            log.info("退款成功: paymentOrderId={}, refundAmount={}", fPaymentOrderId, refundAmount);
            return R.success("退款成功");
        } finally {
            if (refundLockValue != null) {
                unlockRefundLock(refundLockKey, refundLockValue);
            }
        }
    }

    /**
     * 校验退款请求的租户归属/状态机/金额上限（等价抽取，降低方法长度）。
     *
     * @return 校验失败时返回错误响应；通过时返回 null
     */
    private R<String> validateRefundRequest(RefundRequestDTO dto, PaymentOrder paymentOrder, BigDecimal refundAmount,
            BigDecimal paymentAmount) {
        // 租户归属校验（兜底租户拦截器在 tenantId 为 null 时跳过过滤的极端情况）
        Long refundTenantId = BaseContext.getCurrentTenantId();
        if (refundTenantId == null || !refundTenantId.equals(paymentOrder.getTenantId())) {
            log.warn("退款越权拦截：employee 尝试退款非本租户支付订单 paymentOrderId={}, orderTenant={}, curTenant={}",
                    dto.getPaymentOrderId(), paymentOrder.getTenantId(), refundTenantId);
            return R.error("无权操作其他租户的支付订单");
        }
        // 支付单状态机校验：仅 SUCCESS 可退款（禁止对 PENDING/FAIL/已退款重复退款）
        if (!STATUS_SUCCESS.equals(paymentOrder.getStatus())) {
            return R.error("支付订单状态不允许退款（当前状态：" + paymentOrder.getStatus() + "）");
        }
        // 金额校验必须 fail-closed：支付单金额缺失属数据异常，拒绝退款而非跳过校验
        // （若短路放行，"单次上限"与"累计上限"两道防线同时失效，可对单笔支付单超额退款）
        if (paymentAmount == null) {
            log.warn("支付单金额缺失，拒绝退款：paymentOrderId={}", paymentOrder.getId());
            return R.error("支付金额异常，无法退款");
        }
        if (refundAmount.compareTo(paymentAmount) > 0) {
            return R.error("退款金额不能大于支付金额（支付金额：" + paymentAmount + "元）");
        }
        // 累计退款金额粗校验（事务内还会二次校验防并发）
        BigDecimal alreadyRefunded = refundRecordService.sumRefundedAmount(paymentOrder.getId());
        if (alreadyRefunded.add(refundAmount).compareTo(paymentAmount) > 0) {
            return R.error("累计退款金额超过支付金额（已退：" + alreadyRefunded + "元）");
        }
        return null;
    }

    /**
     * 调用渠道退款（等价抽取，事务外）。
     *
     * @return 失败时返回错误响应；成功时返回 null
     */
    private R<String> callChannelRefund(PaymentOrder lockedOrder, BigDecimal refundAmount, String reason,
            String refundNo, Long paymentOrderId) {
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(lockedOrder.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(lockedOrder.getChannelTradeNo());
        refundRequest.setAmount(refundAmount);
        refundRequest.setReason(reason);
        refundRequest.setOutRequestNo(refundNo);
        RefundResponse refundResponse;
        try {
            refundResponse = paymentChannel.refund(refundRequest);
        } catch (Exception e) {
            // 渠道调用异常（钱未出）——留对账待办痕迹，供 RefundReconcileTask 扫描告警人工退款
            log.error("【严重】退款渠道调用异常，需人工处理！paymentOrderId={}, refundAmount={}, reason={}",
                    paymentOrderId, refundAmount, e.getMessage(), e);
            recordReconcileTraceSafely(paymentOrderId, refundAmount, "[对账待办]渠道退款调用异常待人工");
            return R.error("退款渠道调用失败，请稍后重试");
        }
        if (refundResponse == null || !refundResponse.isSuccess()) {
            String errMsg = refundResponse != null ? refundResponse.getErrorMsg() : "无响应";
            log.warn("退款失败: paymentOrderId={}, errorMsg={}", paymentOrderId, errMsg);
            recordReconcileTraceSafely(paymentOrderId, refundAmount, "[对账待办]渠道退款被拒绝待人工：" + errMsg);
            return R.error("退款失败: " + errMsg);
        }
        return null;
    }

    /**
     * 事务内更新本地数据：行锁二次校验 + 创建退款记录 + 全额退款联动（等价抽取，降低方法长度）。
     *
     * @return 本地落库失败时返回错误响应；成功时返回 null
     */
    private R<String> persistRefundTransactionally(Long paymentOrderId, BigDecimal refundAmount, String reason,
            String refundNo) {
        final Long fPaymentOrderId = paymentOrderId;
        final BigDecimal fRefundAmount = refundAmount;
        final String fReason = reason;
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                // 重新查询支付单（防并发退款）
                PaymentOrder latest = paymentOrderService.getById(fPaymentOrderId);
                if (latest == null || !STATUS_SUCCESS.equals(latest.getStatus())) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                // 事务内二次累计退款校验（用 SELECT ... FOR UPDATE 锁定支付单行，阻塞并发退款）
                // 先对 payment_order 行加排他锁，再查询累计退款——两阶段串行化防突破上限
                BigDecimal lockedAmount = paymentOrderMapper.selectPaymentAmountForUpdate(latest.getId(),
                        STATUS_SUCCESS);
                // 行锁后读到的金额是权威值；为 null 属数据异常，fail-closed 拒绝而非跳过校验
                if (lockedAmount == null) {
                    throw new CustomException("支付金额异常，退款失败");
                }
                BigDecimal latestAmount = latest.getAmount();
                if (latestAmount == null) {
                    throw new CustomException("支付金额异常，退款失败");
                }
                if (lockedAmount.compareTo(latestAmount) != 0) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                BigDecimal refunded = refundRecordService.sumRefundedAmount(latest.getId());
                if (refunded.add(fRefundAmount).compareTo(latestAmount) > 0) {
                    throw new CustomException("累计退款金额超过支付金额（已退：" + refunded + "元）");
                }
                // 创建退款记录并标记成功（渠道已确认退款，修复原先记录永远停留在 PENDING 的问题）。
                // refundNo 提前生成作为渠道幂等键 out_request_no，此处复用同一单号，
                // 保证本地 refund_no 与渠道 out_request_no 一一对应可直接对账。
                RefundRecord record = refundRecordService.createRefund(latest.getId(), fRefundAmount, fReason,
                        refundNo);
                refundRecordService.markRefundSuccess(record.getRefundNo());
                // 判断是否全额退款：累计已退 + 本次 == 支付金额
                boolean isFull = refunded.add(fRefundAmount).compareTo(latestAmount) == 0;
                // CAS 更新支付单状态：仅全额退款时 SUCCESS -> REFUND（原子更新防覆盖）
                if (isFull) {
                    boolean updated = paymentOrderService.lambdaUpdate()
                            .eq(PaymentOrder::getId, latest.getId())
                            .eq(PaymentOrder::getStatus, STATUS_SUCCESS)
                            .set(PaymentOrder::getStatus, STATUS_REFUND)
                            .set(PaymentOrder::getUpdateTime, LocalDateTime.now())
                            .update();
                    if (!updated) {
                        throw new CustomException("支付单状态已变更，退款失败");
                    }
                }
                // 联动更新业务订单状态（状态机校验：仅已付款状态可流转为已退款；已退款幂等跳过）
                if (isFull) {
                    Orders order = orderService.getById(latest.getOrderId());
                    if (order != null) {
                        updateOrderOnFullRefund(order, latest);
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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("【严重】渠道退款成功但本地落库失败，对账痕迹持久化也失败: paymentOrderId={}, refundAmount={}",
                        fPaymentOrderId, fRefundAmount, traceEx);
            }
            // 2. 主日志告警
            log.error("【严重】渠道退款成功但本地数据更新失败，需人工核对对账！paymentOrderId={}, refundAmount={}, reason={}",
                    fPaymentOrderId, fRefundAmount, e.getMessage(), e);
            return R.error("退款已提交渠道但本地更新失败，请联系管理员核对");
        }
        return null;
    }

    /**
     * 全额退款时联动更新业务订单状态并回退会员权益（等价抽取，降低嵌套）。
     *
     * @param order 关联业务订单
     * @param latest 支付单
     */
    private void updateOrderOnFullRefund(Orders order, PaymentOrder latest) {
        Integer curStatus = order.getStatus();
        if (curStatus != null && Arrays.asList(
                Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING, Orders.STATUS_COMPLETED).contains(curStatus)) {
            // 修复 P2-5：CAS 乐观锁更新订单状态，防止并发退款覆盖
            LambdaUpdateWrapper<Orders> orderUpdateWrapper = new LambdaUpdateWrapper<>();
            orderUpdateWrapper.eq(Orders::getId, order.getId()).eq(Orders::getStatus, curStatus);
            Orders updateEntity = new Orders();
            updateEntity.setStatus(Orders.STATUS_REFUNDED);
            updateEntity.setUpdateTime(java.time.LocalDateTime.now());
            if (!orderService.update(updateEntity, orderUpdateWrapper)) {
                log.warn("订单状态已变更，跳过联动退款更新: orderId={}, expectedStatus={}", latest.getOrderId(), curStatus);
                return;
            }
            // 全额退款后回退会员权益（积分回退 + 优惠券恢复）
            try {
                memberRewardService.reverseRewards(latest.getOrderId(), latest.getTenantId());
                log.info("[会员权益回退] 退款触发权益回退: orderId={}, tenantId={}", latest.getOrderId(), latest.getTenantId());
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[会员权益回退] 退款后权益回退失败，需人工核查: orderId={}", latest.getOrderId(), e);
            }
            // 同步回补菜品+原料库存（失败仅日志，不阻断退款，由库存补偿任务兜底）
            try {
                orderStockRefundService.restoreForOrder(latest.getOrderId());
            } catch (Exception e) {
                log.error("[库存回补] 退款触发库存回补异常，待补偿任务兜底: orderId={}", latest.getOrderId(), e);
            }
            log.info("退款成功联动更新订单: orderId={}, orderStatus=已退款", latest.getOrderId());
        } else if (curStatus != null && curStatus == Orders.STATUS_REFUNDED) {
            log.info("订单已为已退款状态，幂等跳过联动更新: orderId={}", latest.getOrderId());
        } else {
            log.warn("订单状态不允许退款流转，跳过联动更新: orderId={}, currentStatus={}", latest.getOrderId(), curStatus);
        }
    }

    /**
     * 尝试获取退款分布式锁（与 {@code PaymentOrderServiceImpl.tryLock} 同模式）。
     * @param lockKey 锁Key
     * @return 锁值（UUID），Redis 不可用或被占用返回 null（降级 DB+渠道幂等兜底）
     */
    private String tryRefundLock(String lockKey) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, REFUND_LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("退款获取分布式锁失败，降级 DB+渠道幂等兜底: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放退款分布式锁（Lua 脚本原子操作：比对锁值后才删除，防止误删他人锁）。
     * @param lockKey 锁Key
     * @param lockValue 锁值（UUID）
     */
    private void unlockRefundLock(String lockKey, String lockValue) {
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
     * reason 以 {@code [对账待办]} 前缀标识，供 RefundReconcileTask 扫描告警。
     * trace 自身失败仅 log.error，不阻断调用方流程。
     * </p>
     */
    private void recordReconcileTraceSafely(Long paymentOrderId, BigDecimal amount, String reason) {
        try {
            refundRecordService.recordReconcileTrace(paymentOrderId, amount, reason);
        } catch (Exception traceEx) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("【严重】退款失败对账痕迹持久化失败，需人工核查: paymentOrderId={}, amount={}",
                    paymentOrderId, amount, traceEx);
        }
    }

    /**
     * 根据交易号查询支付订单状态
     * @param tradeNo 交易号
     * @return 支付订单信息
     */
    @GetMapping("/query/{tradeNo}")
    @RequireEmployee
    @Operation(summary = "查询支付状态", description = "根据交易号查询支付订单状态")
    public R<PaymentOrder> query(
                        @Parameter(description = "交易号", required = true) @PathVariable String tradeNo) {
        PaymentOrder po = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            return R.error("支付订单不存在");
        }
        // 租户归属校验，确保只能查询本租户支付订单（兜底租户拦截器在 tenantId 为 null 时跳过过滤）
        Long queryTenantId = BaseContext.getCurrentTenantId();
        if (queryTenantId != null && !queryTenantId.equals(po.getTenantId())) {
            return R.error("无权查询其他租户的支付订单");
        }
        return R.success(po);
    }

    /**
     * 分页查询。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param orderId 参数 orderId
     * @param channel 参数 channel
     * @param status 参数 status
     * @param beginTime 参数 beginTime
     * @param endTime 参数 endTime
     * @return 返回结果
     */
    @RequireEmployee
    @GetMapping("/page")
    @Operation(summary = "分页查询支付订单", description = "分页查询支付订单列表，支持按订单ID、渠道、状态、时间范围筛选")
    public R<Page<PaymentOrder>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "订单ID") Long orderId,
            @Parameter(description = "支付渠道：ALIPAY-支付宝, WECHAT-微信") String channel,
            @Parameter(description = "支付状态：PENDING-待支付, SUCCESS-成功, FAIL-失败, REFUND-已退款") String status,
            @Parameter(description = "开始时间（yyyy-MM-dd HH:mm:ss）") String beginTime,
            @Parameter(description = "结束时间（yyyy-MM-dd HH:mm:ss）") String endTime) {
        Page<PaymentOrder> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<PaymentOrder> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PaymentOrder::getOrderId, orderId);
        qw.eq(channel != null && !channel.isEmpty(), PaymentOrder::getChannel, channel);
        if (status != null && !status.isEmpty()) {
            switch (status) {
                case "待支付": case "PENDING":   qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_PENDING); break;
                case "成功":   case "SUCCESS":   qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_SUCCESS); break;
                case "失败":   case "FAIL": case "FAILED": qw.eq(PaymentOrder::getStatus, PaymentOrder
                        .STATUS_FAIL); break;
                case "已退款": case "REFUND": case "REFUNDED": qw.eq(PaymentOrder::getStatus, PaymentOrder
                        .STATUS_REFUND); break;
                default:                        qw.eq(PaymentOrder::getStatus, status); break;
            }
        }
        if (beginTime != null && !beginTime.isEmpty()) {
            qw.ge(PaymentOrder::getCreatedTime, java.time.LocalDateTime.parse(beginTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            qw.le(PaymentOrder::getCreatedTime, java.time.LocalDateTime.parse(endTime,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        qw.orderByDesc(PaymentOrder::getCreatedTime);
        paymentOrderService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 对账待办统计：查询 reason 以 [对账待办] 开头的退款记录数量
     */
    @RequireEmployee
    @GetMapping("/reconcile/pending-count")
    @Operation(summary = "对账待办数量", description = "统计待人工核对的对账待办退款记录数量")
    public R<Map<String, Object>> reconcilePendingCount() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(RefundRecord::getTenantId, tenantId)
          .likeRight(RefundRecord::getReason, "[对账待办]")
          .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode());
        long count = refundRecordService.count(qw);
        Map<String, Object> result = new HashMap<>();
        result.put("pendingCount", count);
        return R.success(result);
    }

    /**
     * 清除 Dashboard 缓存（退款后调用，确保概览数据实时准确）
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


