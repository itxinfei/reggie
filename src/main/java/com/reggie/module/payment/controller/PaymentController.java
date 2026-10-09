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
import com.fasterxml.jackson.core.type.TypeReference;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.payment.channel.MapNotifyCapable;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.notify.NotifyRequest;
import com.reggie.module.payment.channel.notify.NotifyResult;
import com.reggie.module.payment.channel.notify.NotifyUrlRouter;
import com.reggie.module.payment.channel.notify.RealNotifyCapable;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
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
    private com.reggie.module.payment.config.PaymentConfigProperties paymentConfigProperties;

    @Autowired
    private RefundService refundService;

    @Autowired
    private com.reggie.module.subsidy.service.SubsidyService subsidyService;

    /** 退款异步回调处理（微信退款终态通知） */
    @Autowired
    private com.reggie.module.payment.service.impl.RefundCallbackService refundCallbackService;

    /**
     * 创建支付订单
     * @param dto 支付请求参数
     * @return 支付渠道响应（支付链接或二维码）
     */
    @PostMapping("/pay")
    @RateLimit(maxRequestsPerSecond = 3, type = RateLimitType.USER)
    @Operation(summary = "创建支付订单", description = "创建支付订单并调用支付渠道生成支付链接或二维码")
    public R<PayResponse> pay(
            @Parameter(description = "支付请求参数", required = true) @Validated @RequestBody PayRequestDTO dto,
            javax.servlet.http.HttpServletRequest httpRequest) {
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
        // 归属校验：C 端用户只能对本人的订单发起支付（0 元单分支同样受此保护，防任意代付置已支付）
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null || !currentUserId.equals(order.getUserId())) {
            return R.error("无权操作该订单");
        }
        // 校验订单为待付款状态
        if (!Objects.equals(order.getStatus(), Orders.STATUS_PENDING_PAY)) {
            return R.error("订单状态不允许支付");
        }
        // 防御性 null 检查：order.amount 可能在数据库中为 null（历史数据或绕过校验）
        BigDecimal payAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
        // 餐补支付（SUBSIDY）：同步渠道——原子扣减餐补余额后直接走支付成功链路，
        // 复用 handlePaymentSuccess 下游（订单流转/KDS/拼团/取消自动退款），无需异步回调
        if ("SUBSIDY".equalsIgnoreCase(dto.getChannel())) {
            return payBySubsidy(order, payAmount, currentUserId, currentTenantId);
        }
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 0 元订单：优惠券/折扣将实付压到 0，自动完成支付，不调用渠道。
            // 双表更新收敛到 PaymentOrderService.completeZeroAmountPayment（单一事务，防支付单/订单部分更新）。
            paymentOrderService.completeZeroAmountPayment(dto.getOrderId());
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
        // payType 由前端按场景选择（不传渠道自行默认）；clientIp 只从 HTTP 请求解析，不采信前端
        request.setPayType(dto.getPayType());
        request.setClientIp(resolveClientIp(httpRequest));
        PayResponse response = paymentChannel.createOrder(request);
        // 回填商户支付单号：沙箱(mock-mode)收银台需用它作为 out_trade_no 发起模拟支付回调
        response.setTradeNo(paymentOrder.getTradeNo());
        // 标明当前是否 mock 渠道，C 端据此区分真实扫码轮询与沙箱模拟回调
        response.setMockMode(paymentConfigProperties.isMockMode());

        return R.success(response);
    }

    /**
     * 解析客户端真实 IP：依次取 X-Forwarded-For 首个非空、X-Real-IP、远端地址。
     * 仅用于支付风控上报；多级代理时 X-Forwarded-For 首个为最初客户端。
     */
    private String resolveClientIp(javax.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
            // 逗号分隔，第一个为最初客户端
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.trim().isEmpty()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 餐补支付：扣减餐补余额（原子 + 流水幂等）→ 推进支付单/订单到已支付。
     * <p>扣减失败（余额不足等）时把支付单置为 FAILED，避免残留 PENDING 支付单。</p>
     */
    private R<PayResponse> payBySubsidy(Orders order, BigDecimal payAmount, Long userId, Long tenantId) {
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // 0 元单：无需扣餐补，直接走 0 元支付闭环
            paymentOrderService.completeZeroAmountPayment(order.getId());
            PayResponse zeroPayResponse = new PayResponse();
            zeroPayResponse.setSuccess(true);
            zeroPayResponse.setRawResponse("0元订单自动完成支付");
            return R.success(zeroPayResponse);
        }
        PaymentOrder paymentOrder = paymentOrderService.createPaymentOrder(order.getId(),
                "SUBSIDY", payAmount);
        try {
            subsidyService.consumeForOrder(userId, tenantId, order.getId(), payAmount,
                    paymentOrder.getTradeNo());
        } catch (CustomException e) {
            // 余额不足等失败：不调 handlePaymentFail（那会联动取消订单）——
            // 保持订单待付款与支付单 PENDING，顾客可充值后重试或改用其他渠道；
            // 失败尝试未写核销流水，同 tradeNo 重试仍会正常扣款
            log.info("[餐补支付] 扣减失败，保持待付款: orderId={}, err={}", order.getId(), e.getMessage());
            return R.error(e.getMessage());
        }
        paymentOrderService.handlePaymentSuccess(paymentOrder.getTradeNo(), "SUBSIDY");
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setPayType("SUBSIDY");
        response.setTradeNo(paymentOrder.getTradeNo());
        log.info("[餐补支付] 完成: orderId={}, tradeNo={}, amount={}",
                order.getId(), paymentOrder.getTradeNo(), payAmount);
        return R.success(response);
    }

    /**
     * 接收支付渠道的异步通知。
     * <p>
     * mock 模式：回调为 JSON Map，走存量表单渠道链路，返回 {@code R}（开发/演示/测试）；
     * 真实模式：按 notify_url 上的 tenantId 路由租户，用官方 SDK 验签/解密，
     * 返回渠道原生 ACK（微信 {"code":"SUCCESS"}、支付宝纯文本 success）。
     * </p>
     *
     * @param httpRequest HTTP 请求（原始 body + 头 + 路由参数）
     * @param channel     支付渠道：WECHAT / ALIPAY
     * @return mock 返回 R；真实返回 ResponseEntity 原生 ACK
     */
    @PostMapping("/notify/{channel}")
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.IP)
    @Operation(summary = "支付回调通知", description = "接收支付渠道的异步通知，更新订单支付状态")
    public Object notify(HttpServletRequest httpRequest,
                         @Parameter(description = "支付渠道：WECHAT-微信、ALIPAY-支付宝", required = true)
                         @PathVariable String channel) {
        String body = readRequestBody(httpRequest);

        if (paymentConfigProperties.isMockMode()) {
            // mock：JSON body → Map，沿用存量表单回调链路
            Map<String, String> params = parseJsonBody(body);
            return notifyMock(channel, params);
        }

        // 真实：先按 notify_url 的 tenantId 路由租户（微信密文无法在选对密钥前提取单号）
        Long routeTenant = NotifyUrlRouter.readTenant(httpRequest);
        if (routeTenant == null) {
            log.warn("真实回调缺少租户路由参数：channel={}", channel);
            return realAck(channel, false, "缺少租户路由参数");
        }
        PaymentChannel paymentChannel =
                paymentChannelFactory.getChannelForTenantNullable(routeTenant, channel);
        if (!(paymentChannel instanceof RealNotifyCapable)) {
            log.warn("真实回调渠道未配置或不可用：tenant={}, channel={}", routeTenant, channel);
            return realAck(channel, false, "渠道未配置");
        }
        RealNotifyCapable capable = (RealNotifyCapable) paymentChannel;
        NotifyRequest notifyRequest = NotifyRequest.from(httpRequest, body);
        NotifyResult result = capable.parsePayment(notifyRequest);
        if (!result.isSuccess()) {
            log.warn("真实回调验签/业务失败：tenant={}, channel={}, err={}",
                    routeTenant, channel, result.getErrorMsg());
            return realAck(channel, false, result.getErrorMsg());
        }
        String tradeNo = result.getTradeNo();
        if (tradeNo == null || tradeNo.trim().isEmpty()) {
            return realAck(channel, false, "缺少 out_trade_no");
        }
        PaymentOrder exist = paymentOrderService.selectByTradeNoIgnoreTenant(tradeNo);
        if (exist == null) {
            return realAck(channel, false, "交易号不存在");
        }
        if (!channel.equalsIgnoreCase(exist.getChannel())) {
            return realAck(channel, false, "渠道不匹配");
        }
        // 金额 fail-closed 比对：回调金额必须存在且与支付单一致，缺失/不一致一律拒绝
        // （2026-09-30 修复：原实现 amount==null 时跳过比对放行，与 mock 路径口径不一致）
        if (result.getAmount() == null) {
            log.warn("真实回调金额缺失，拒绝：tradeNo={}", tradeNo);
            return realAck(channel, false, "回调金额缺失");
        }
        BigDecimal existAmount = exist.getAmount();
        if (existAmount == null) {
            return realAck(channel, false, "支付金额非法");
        }
        if (result.getAmount().compareTo(existAmount) != 0) {
            log.warn("真实回调金额不一致：notify={}, order={}, tradeNo={}",
                    result.getAmount(), existAmount, tradeNo);
            return realAck(channel, false, "金额不一致");
        }
        paymentOrderService.handlePaymentSuccess(tradeNo, result.getChannelTradeNo());
        log.info("真实支付回调处理成功 channel={}, tradeNo={}", channel, tradeNo);
        return realAck(channel, true, null);
    }

    /**
     * 退款异步回调：微信 APIv3 退款终态通知（支付宝退款同步即确定终态，不推送此通知）。
     * <p>
     * 流程与支付回调一致：按 notify_url 的 tenantId 路由租户 → 渠道验签/解密（parseRefund）
     * → 交 {@code RefundCallbackService} 做终态化与全额联动，返回渠道原生 ACK。
     * </p>
     *
     * @param httpRequest 原始请求（微信回调头 + 加密 body）
     * @param channel     渠道（当前仅 WECHAT）
     * @return 渠道原生 ACK（微信 {@code {"code":"SUCCESS"/"FAIL"}}）
     */
    @PostMapping("/refund-notify/{channel}")
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.IP)
    @Operation(summary = "退款回调通知", description = "接收支付渠道的退款异步通知，定退款终态并联动订单/库存/积分")
    public ResponseEntity<String> refundNotify(HttpServletRequest httpRequest,
            @Parameter(description = "支付渠道：WECHAT-微信", required = true)
            @PathVariable String channel) {
        String body = readRequestBody(httpRequest);
        if (paymentConfigProperties.isMockMode()) {
            // mock 模式不存在真实退款回调，回成功 ACK 且不产生任何副作用
            return realAck(channel, true, null);
        }
        Long routeTenant = NotifyUrlRouter.readTenant(httpRequest);
        if (routeTenant == null) {
            return realAck(channel, false, "缺少租户路由参数");
        }
        PaymentChannel paymentChannel =
                paymentChannelFactory.getChannelForTenantNullable(routeTenant, channel);
        if (!(paymentChannel instanceof RealNotifyCapable)) {
            return realAck(channel, false, "渠道未配置");
        }
        NotifyRequest notifyRequest = NotifyRequest.from(httpRequest, body);
        com.reggie.module.payment.channel.notify.RefundNotifyResult result =
                ((RealNotifyCapable) paymentChannel).parseRefund(notifyRequest);
        if (result == null || !result.isSuccess()) {
            String errMsg = result != null ? result.getErrorMsg() : "退款回调解析失败";
            log.warn("[退款回调] 验签/解析失败：tenant={}, channel={}, err={}",
                    routeTenant, channel, errMsg);
            return realAck(channel, false, errMsg);
        }
        boolean handled = refundCallbackService.handleRefundNotify(result);
        if (!handled) {
            // 本地缺记录/状态未确定：回失败 ACK 让微信按策略重试
            return realAck(channel, false, "本地暂未处理，等待重试");
        }
        return realAck(channel, true, null);
    }

    /**
     * mock 模式回调处理：存量表单渠道链路，完整保留原有全部安全校验。
     */
    private R<String> notifyMock(String channel, Map<String, String> params) {
        // 回调场景用 getChannelNullable：未知/空渠道返回 200 + 业务错误码（而非抛异常触发 500）
        PaymentChannel paymentChannel = paymentChannelFactory.getChannelNullable(channel);
        if (paymentChannel == null) {
            log.warn("支付回调渠道不支持，拒绝处理：channel={}", channel);
            return R.error("不支持的支付通道");
        }
        if (!(paymentChannel instanceof MapNotifyCapable)) {
            log.warn("支付渠道不支持表单回调，拒绝处理：channel={}", channel);
            return R.error("不支持的回调类型");
        }
        MapNotifyCapable notifyChannel = (MapNotifyCapable) paymentChannel;
        // 签名校验：禁止直接信任未验签的回调参数（防回调伪造）
        if (!notifyChannel.verifyNotifySign(params)) {
            log.warn("支付回调签名校验失败：channel={}, params={}", channel, params);
            return R.error("回调签名校验失败");
        }
        String tradeNo = params.get("out_trade_no");
        if (tradeNo == null || tradeNo.trim().isEmpty()) {
            log.warn("支付回调缺少 out_trade_no 参数，channel={}", channel);
            return R.error("回调缺少 out_trade_no 参数");
        }
        // 防御性校验：tradeNo 必须对应真实存在的支付单
        PaymentOrder exist = paymentOrderService.selectByTradeNoIgnoreTenant(tradeNo);
        if (exist == null) {
            log.warn("支付回调 tradeNo 不存在，拒绝处理：channel={}, tradeNo={}", channel, tradeNo);
            return R.error("回调交易号不存在");
        }
        // 渠道一致性校验：防止用 WECHAT 回调参数（含有效签名）伪造 ALIPAY 支付单
        if (!channel.equals(exist.getChannel())) {
            log.warn("支付回调渠道不一致，拒绝处理：pathChannel={}, orderChannel={}, tradeNo={}",
                    channel, exist.getChannel(), tradeNo);
            return R.error("支付渠道不匹配");
        }
        // 金额一致性校验：微信回调 total_fee（分），支付宝 total_amount（元）
        String channelFeeField = "WECHAT".equalsIgnoreCase(channel) ? "total_fee" : "total_amount";
        String notifyAmountStr = params.get(channelFeeField);
        if (notifyAmountStr != null && !notifyAmountStr.trim().isEmpty()) {
            try {
                BigDecimal notifyAmount;
                if ("WECHAT".equalsIgnoreCase(channel)) {
                    notifyAmount = new BigDecimal(notifyAmountStr).divide(new BigDecimal("100"), 2,
                            java.math.RoundingMode.HALF_UP);
                } else {
                    notifyAmount = new BigDecimal(notifyAmountStr);
                }
                BigDecimal existAmount = exist.getAmount();
                // fail-closed：支付单金额缺失属数据异常，拒绝而非放行
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
        PayResponse response = notifyChannel.handleNotify(params);
        if (response.isSuccess()) {
            paymentOrderService.handlePaymentSuccess(tradeNo, response.getChannelTradeNo());
            return R.success("回调处理成功");
        }
        log.warn("支付回调处理失败：channel={}, errorMsg={}", channel, response.getErrorMsg());
        return R.error("回调处理失败");
    }

    /**
     * 构造真实渠道原生 ACK。
     * 微信返回 {"code":"SUCCESS"/"FAIL"}；支付宝返回纯文本 success / failure。
     */
    private ResponseEntity<String> realAck(String channel, boolean success, String message) {
        if ("WECHAT".equalsIgnoreCase(channel)) {
            StringBuilder json = new StringBuilder();
            json.append("{\"code\":\"").append(success ? "SUCCESS" : "FAIL").append("\"");
            if (!success && message != null && !message.trim().isEmpty()) {
                json.append(",\"message\":\"").append(jsonEscape(message)).append("\"");
            }
            json.append("}");
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json.toString());
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN)
                .body(success ? "success" : "failure");
    }

    /** 读取回调原始 body（保持换行，供微信验签 body 一致性）。 */
    private String readRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            log.error("读取回调请求体失败", e);
            return "";
        }
    }

    /** 把 mock 回调的 JSON body 解析为 Map；失败返回空 Map。 */
    private Map<String, String> parseJsonBody(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return new HashMap<>();
            }
            return ObjectMapperHolder.getDefault().readValue(body,
                    new TypeReference<Map<String, String>>() {
                    });
        } catch (Exception e) {
            log.warn("解析回调 JSON body 失败 err={}", e.getMessage());
            return new HashMap<>();
        }
    }

    /** JSON 字符串转义（错误 ACK 的 message，防引号/反斜杠破坏 JSON）。 */
    private String jsonEscape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * 退款分析（当前租户）
     * <p>返回退款总数/成功/退款中/失败、成功退款总额、退款原因 TOP5，供报表页退款分析。</p>
     *
     * @return 退款分析结果
     */
    @RequireEmployee
    @GetMapping("/refund/stats")
    @Operation(summary = "退款分析", description = "当前租户退款统计与退款原因TOP5；可选 startDate/endDate(yyyy-MM-dd) 按区间统计，不传为累计口径")
    public R<Map<String, Object>> refundStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.success(refundRecordService.getRefundAnalysis(
                BaseContext.getCurrentTenantId(), startDate, endDate));
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
     * <p>编排收敛至 {@code RefundService.executeUserRefundByRecord}（唯一实现）：Redis 锁 → 渠道退款（事务外）
     * → 复用既有售后单落库 + 全额联动（同一事务）。本端点仅做身份/参数转发与结果包装。</p>
     *
     * @param refundId 售后记录ID
     * @return 退款结果
     */
    @RequireEmployee
    @PostMapping("/refund/user/execute")
    @Operation(summary = "执行售后退款", description = "审核通过后触发渠道退款，完成售后闭环")
    public R<String> executeUserRefund(
            @Parameter(description = "售后记录ID", required = true) @RequestParam Long refundId) {
        try {
            return R.success(refundService.executeUserRefundByRecord(BaseContext.getCurrentTenantId(), refundId));
        } catch (CustomException e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 申请退款
     * <p>退款编排收敛至 {@code RefundService.refundByPaymentOrder}（唯一实现）：校验 → 离线通道本地记账 /
     * 在线通道「Redis 锁 → 渠道 HTTP（事务外）→ REQUIRED 编程式事务落库 + 全额联动」。本端点仅做 DTO 参数
     * 校验、转发调用与结果包装，不再持有支付编排副本。</p>
     * @param dto 退款请求参数
     * @return 退款结果
     */
    @RequireEmployee
    @PostMapping("/refund")
    @Operation(summary = "申请退款", description = "申请退款并调用支付渠道处理退款流程")
    public R<String> refund(
            @Parameter(description = "退款请求参数", required = true) @Validated @RequestBody RefundRequestDTO dto) {
        try {
            return R.success(refundService.refundByPaymentOrder(BaseContext.getCurrentTenantId(),
                    dto.getPaymentOrderId(), dto.getAmount(), dto.getReason()));
        } catch (CustomException e) {
            return R.error(e.getMessage());
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
     * C 端用户查询支付状态（收银台轮询用）。
     * <p>
     * 与员工端点 {@code /query/{tradeNo}} 不同：不要求员工身份，但必须校验支付单
     * 关联订单的 userId == 当前登录用户，防止用户凭 tradeNo 探测他人支付状态。
     * 返回精简字段（状态 + 金额 + 场景标记），不暴露支付单实体。
     * </p>
     *
     * @param tradeNo 商户支付单号
     * @return status/orderId/channel/amount/mockMode
     */
    @GetMapping("/user/query/{tradeNo}")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "用户查询支付状态", description = "C端收银台轮询支付结果，校验订单归属当前用户")
    public R<Map<String, Object>> userQuery(
            @Parameter(description = "商户支付单号", required = true) @PathVariable String tradeNo) {
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return R.error("登录状态异常，请重新登录");
        }
        PaymentOrder po = paymentOrderService.lambdaQuery()
            .eq(PaymentOrder::getTradeNo, tradeNo).one();
        if (po == null) {
            return R.error("支付订单不存在");
        }
        Orders order = orderService.getById(po.getOrderId());
        if (order == null) {
            return R.error("支付订单不存在");
        }
        // 归属校验：支付单关联订单必须属于当前用户。失败时对外统一文案，避免支付单存在性探测
        if (!currentUserId.equals(order.getUserId())) {
            log.warn("用户查询支付单归属校验失败: userId={}, tradeNo={}, orderUserId={}",
                    currentUserId, tradeNo, order.getUserId());
            return R.error("支付订单不存在");
        }
        Long queryTenantId = BaseContext.getCurrentTenantId();
        if (queryTenantId != null && !queryTenantId.equals(po.getTenantId())) {
            log.warn("用户查询支付单租户校验失败: userId={}, tradeNo={}, payTenantId={}",
                    currentUserId, tradeNo, po.getTenantId());
            return R.error("支付订单不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("status", po.getStatus());
        result.put("orderId", po.getOrderId());
        result.put("channel", po.getChannel());
        result.put("amount", po.getAmount());
        result.put("mockMode", paymentConfigProperties.isMockMode());
        return R.success(result);
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
}


