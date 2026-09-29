package com.reggie.module.payment.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.RechargeRecordService;
import com.reggie.module.payment.channel.PayRequest;
import com.reggie.module.payment.channel.PayResponse;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.config.PaymentConfigProperties;
import com.reggie.module.payment.dto.RechargePayDTO;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * 会员充值在线支付（P1-4）。
 *
 * <p>独立于强耦合 Orders 的 {@code /api/payment/pay}：对 PENDING 充值单创建
 * bizType=RECHARGE 的支付单并调起渠道；支付成功回调由
 * {@code PaymentOrderServiceImpl.handlePaymentSuccess} 按 bizType 联动
 * {@code RechargeRecordService.handleRechargePaid} 入账（本金+赠送）。</p>
 *
 * @author reggie
 * @since 2026-09-29
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/recharge")
@Tag(name = "会员充值支付", description = "对充值单发起微信/支付宝在线支付")
public class RechargePaymentController {

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private PaymentChannelFactory paymentChannelFactory;

    @Autowired
    private PaymentConfigProperties paymentConfigProperties;

    @Autowired
    private RechargeRecordService rechargeRecordService;

    /**
     * 发起充值支付。
     * @param dto 充值支付请求
     * @param httpRequest HTTP 请求（解析客户端IP）
     * @return 渠道支付响应
     */
    @PostMapping("/pay")
    @RateLimit(maxRequestsPerSecond = 3, type = RateLimitType.USER)
    @Operation(summary = "会员充值支付", description = "对PENDING充值单发起在线支付，支付成功回调自动入账")
    public R<PayResponse> pay(@Valid @RequestBody RechargePayDTO dto, HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return R.error("请先登录");
        }
        RechargeRecord record = rechargeRecordService.getByRechargeNo(dto.getRechargeNo());
        // 不存在 / 非本人一律统一文案，避免充值单存在性探测
        if (record == null || !userId.equals(record.getUserId())) {
            return R.error("充值单不存在");
        }
        if (!RechargeRecord.STATUS_PENDING.equals(record.getStatus())) {
            return R.error("充值单当前状态不允许支付");
        }
        BigDecimal payAmount = record.getAmount();
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return R.error("充值金额异常");
        }
        String channel = dto.getChannel();

        PaymentOrder po = paymentOrderService.createPaymentOrderForBiz(
                PaymentOrder.BIZ_RECHARGE, record.getId(), channel, payAmount);

        PaymentChannel paymentChannel = paymentChannelFactory.getChannel(channel);
        PayRequest request = new PayRequest();
        request.setTradeNo(po.getTradeNo());
        request.setAmount(payAmount);
        request.setSubject("瑞吉会员充值");
        request.setPayType(dto.getPayType());
        request.setClientIp(resolveClientIp(httpRequest));
        PayResponse response = paymentChannel.createOrder(request);
        response.setTradeNo(po.getTradeNo());
        response.setMockMode(paymentConfigProperties.isMockMode());
        log.info("[充值支付] 发起充值支付: rechargeNo={}, channel={}, tradeNo={}",
                record.getRechargeNo(), channel, po.getTradeNo());
        return R.success(response);
    }

    /**
     * 解析客户端真实 IP（X-Forwarded-For 首个 → X-Real-IP → 远端地址）。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty()) {
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
}
