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
import com.reggie.module.dashboard.service.DashboardService;
import com.reggie.module.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Map;

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
    private DashboardService dashboardService;

    @Autowired
    private com.reggie.module.member.service.MemberRewardService memberRewardService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private com.reggie.module.payment.mapper.PaymentOrderMapper paymentOrderMapper;

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
            return R.error("订单金额异常");
        }

        PaymentOrder paymentOrder = paymentOrderService.createPaymentOrder(dto.getOrderId(), dto.getChannel(), payAmount);

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(dto.getChannel());
        PayRequest request = new PayRequest();
        request.setTradeNo(paymentOrder.getTradeNo());
        request.setAmount(payAmount);
        request.setSubject("瑞吉外卖-订单" + dto.getOrderId());
        PayResponse response = paymentChannel.createOrder(request);

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
                        @Parameter(description = "支付渠道：WECHAT-微信、ALIPAY-支付宝", required = true) @PathVariable String channel,
            @Parameter(description = "回调参数") @RequestBody Map<String, String> params) {
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(channel);
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
        // 修复 P2：金额一致性校验 — 回调 total_fee（分）与支付单金额比对，防回调金额篡改
        String totalFee = params.get("total_fee");
        if (totalFee != null && !totalFee.trim().isEmpty()) {
            try {
                // total_fee 单位为分，支付单 amount 单位为元
                BigDecimal notifyAmount = new BigDecimal(totalFee).divide(new BigDecimal("100"), 2,
                        java.math.RoundingMode.HALF_UP);
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
                log.warn("支付回调 total_fee 格式非法，拒绝处理：total_fee={}, tradeNo={}", totalFee, tradeNo);
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
        // 退款金额校验：单次不能超过支付金额
        BigDecimal refundAmount = dto.getAmount();
        BigDecimal paymentAmount = paymentOrder.getAmount();
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

        // === 2. 调用渠道退款（事务外，外部 HTTP 不应被事务包裹） ===
        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(paymentOrder.getChannel());
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setChannelTradeNo(paymentOrder.getChannelTradeNo());
        refundRequest.setAmount(refundAmount);
        refundRequest.setReason(dto.getReason());
        RefundResponse refundResponse = paymentChannel.refund(refundRequest);

        if (!refundResponse.isSuccess()) {
            log.warn("退款失败: paymentOrderId={}, errorMsg={}", dto.getPaymentOrderId(), refundResponse.getErrorMsg());
            return R.error("退款失败: " + refundResponse.getErrorMsg());
        }

        // === 3. 事务内更新本地数据（渠道已退款成功，本地必须落库） ===
        final BigDecimal fRefundAmount = refundAmount;
        final String fReason = dto.getReason();
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                // 重新查询支付单（防并发退款）
                PaymentOrder latest = paymentOrderService.getById(paymentOrder.getId());
                if (latest == null || !STATUS_SUCCESS.equals(latest.getStatus())) {
                    throw new CustomException("支付单状态已变更，退款失败");
                }
                // 事务内二次累计退款校验（用 SELECT ... FOR UPDATE 锁定支付单行，阻塞并发退款）
                // 先对 payment_order 行加排他锁，再查询累计退款——两阶段串行化防突破上限
                BigDecimal lockedAmount = paymentOrderMapper.selectPaymentAmountForUpdate(latest.getId(), STATUS_SUCCESS);
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
                // 创建退款记录并标记成功（渠道已确认退款，修复原先记录永远停留在 PENDING 的问题）
                RefundRecord record = refundRecordService.createRefund(latest.getId(), fRefundAmount, fReason);
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
                        Integer curStatus = order.getStatus();
                        if (curStatus != null && Arrays.asList(
                                Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING, Orders.STATUS_COMPLETED).contains(curStatus)) {
                            // 修复 P2-5：CAS 乐观锁更新订单状态，防止并发退款覆盖
                            LambdaUpdateWrapper<Orders> orderUpdateWrapper = new LambdaUpdateWrapper<>();
                            orderUpdateWrapper.eq(Orders::getId, order.getId())
                                    .eq(Orders::getStatus, curStatus);
                            Orders updateEntity = new Orders();
                            updateEntity.setStatus(Orders.STATUS_REFUNDED);
                            updateEntity.setUpdateTime(java.time.LocalDateTime.now());
                            boolean updated = orderService.update(updateEntity, orderUpdateWrapper);
                            if (!updated) {
                                log.warn("订单状态已变更，跳过联动退款更新: orderId={}, expectedStatus={}",
                                        latest.getOrderId(), curStatus);
                            } else {
                                // 全额退款后回退会员权益（积分回退 + 优惠券恢复）
                                try {
                                    memberRewardService.reverseRewards(latest.getOrderId(), latest.getTenantId());
                                    log.info("[会员权益回退] 退款触发权益回退: orderId={}, tenantId={}", latest.getOrderId(), latest.getTenantId());
                                } catch (Exception e) {
                                    log.error("[会员权益回退] 退款后权益回退失败，需人工核查: orderId={}", latest.getOrderId(), e);
                                }
                                log.info("退款成功联动更新订单: orderId={}, orderStatus=已退款", latest.getOrderId());
                            }
                        } else if (curStatus != null && curStatus == Orders.STATUS_REFUNDED) {
                            log.info("订单已为已退款状态，幂等跳过联动更新: orderId={}", latest.getOrderId());
                        } else {
                            log.warn("订单状态不允许退款流转，跳过联动更新: orderId={}, currentStatus={}",
                                    latest.getOrderId(), curStatus);
                        }
                    }
                }
                return null;
            });
        } catch (CustomException e) {
            // 渠道已退款但本地落库失败——资金已出、数据未同步，必须告警人工核对
            log.error("【严重】渠道退款成功但本地数据更新失败，需人工核对对账！paymentOrderId={}, refundAmount={}, reason={}",
                    dto.getPaymentOrderId(), refundAmount, e.getMessage());
            return R.error("退款已提交渠道但本地更新失败，请联系管理员核对");
        }

        // 退款成功后清除 Dashboard 缓存，确保今日订单/营业额数据实时准确
        clearDashboardCache();
        log.info("退款成功: paymentOrderId={}, refundAmount={}", dto.getPaymentOrderId(), refundAmount);
        return R.success("退款成功");
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
                case "失败":   case "FAIL": case "FAILED": qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_FAIL); break;
                case "已退款": case "REFUND": case "REFUNDED": qw.eq(PaymentOrder::getStatus, PaymentOrder.STATUS_REFUND); break;
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


