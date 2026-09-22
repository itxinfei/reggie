package com.reggie.module.payment.channel.real;

import com.reggie.module.payment.channel.PayRequest;
import com.reggie.module.payment.channel.PayResponse;
import com.reggie.module.payment.channel.PaymentChannel;
import com.reggie.module.payment.channel.RefundRequest;
import com.reggie.module.payment.channel.RefundResponse;
import com.reggie.module.payment.channel.notify.NotifyRequest;
import com.reggie.module.payment.channel.notify.NotifyResult;
import com.reggie.module.payment.channel.notify.NotifyUrlRouter;
import com.reggie.module.payment.channel.notify.RealNotifyCapable;
import com.reggie.module.payment.channel.notify.RefundNotifyResult;
import com.reggie.module.payment.channel.sdk.WechatSdkClients;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.utils.QRCodeUtil;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByIdRequest;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 微信支付 APIv3 真实渠道。
 * <p>
 * 通过 {@code WechatSdkBuilder} 构建的官方 SDK 客户端实现：
 * NATIVE 扫码下单（返回 code_url 并渲染为二维码）、H5 跳转下单、按商户单号查询、申请退款。
 * 实例由 {@code PaymentChannelFactory} 在非 mock 模式下按「租户 + 渠道」创建并缓存，
 * 故本身不是 Spring Bean，构造时注入复用的 SDK 客户端、配置与二维码工具。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
public class WechatV3PayChannel implements PaymentChannel, RealNotifyCapable {

    private static final String CURRENCY_CNY = "CNY";
    private static final String H5_TYPE_WAP = "Wap";

    private final WechatSdkClients clients;
    private final PaymentChannelConfig config;
    private final QRCodeUtil qrCodeUtil;

    public WechatV3PayChannel(WechatSdkClients clients, PaymentChannelConfig config,
                              QRCodeUtil qrCodeUtil) {
        this.clients = clients;
        this.config = config;
        this.qrCodeUtil = qrCodeUtil;
    }

    @Override
    public PayResponse createOrder(PayRequest request) {
        String payType = resolvePayType(request.getPayType());
        log.info("[微信支付] 创建订单 tradeNo={}, amount={}, payType={}",
                request.getTradeNo(), request.getAmount(), payType);
        try {
            if ("H5".equals(payType)) {
                return createH5Order(request);
            }
            return createNativeOrder(request);
        } catch (Exception e) {
            log.error("[微信支付] 下单失败 tradeNo={}, err={}", request.getTradeNo(), errOf(e), e);
            return payFail(errOf(e));
        }
    }

    /**
     * NATIVE 扫码下单：返回 code_url，并渲染为 PNG 二维码 Data URI。
     */
    private PayResponse createNativeOrder(PayRequest request) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(config.getWxAppId());
        prepayRequest.setMchid(config.getWxMchId());
        prepayRequest.setDescription(resolveDescription(request));
        prepayRequest.setOutTradeNo(request.getTradeNo());
        prepayRequest.setNotifyUrl(NotifyUrlRouter.withTenant(config.getPayNotifyUrl(), config.getTenantId()));
        Amount amount = new Amount();
        amount.setTotal(toFen(request.getAmount()));
        amount.setCurrency(CURRENCY_CNY);
        prepayRequest.setAmount(amount);

        PrepayResponse prepayResponse = clients.getNativePayService().prepay(prepayRequest);
        String codeUrl = prepayResponse.getCodeUrl();

        PayResponse response = successResponse("NATIVE");
        response.setPayUrl(codeUrl);
        response.setQrCodeUrl(qrCodeUtil.generateDataUri(codeUrl));
        response.setRawResponse(codeUrl);
        return response;
    }

    /**
     * H5 跳转下单：返回 h5_url，前端直接 location.href 拉起微信客户端。
     */
    private PayResponse createH5Order(PayRequest request) {
        com.wechat.pay.java.service.payments.h5.model.PrepayRequest h5Request =
                new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();
        h5Request.setAppid(config.getWxAppId());
        h5Request.setMchid(config.getWxMchId());
        h5Request.setDescription(resolveDescription(request));
        h5Request.setOutTradeNo(request.getTradeNo());
        h5Request.setNotifyUrl(NotifyUrlRouter.withTenant(config.getPayNotifyUrl(), config.getTenantId()));

        com.wechat.pay.java.service.payments.h5.model.Amount h5Amount =
                new com.wechat.pay.java.service.payments.h5.model.Amount();
        h5Amount.setTotal(toFen(request.getAmount()));
        h5Amount.setCurrency(CURRENCY_CNY);
        h5Request.setAmount(h5Amount);

        // H5 支付必须提供场景信息：用户终端 IP 与 H5 场景类型，否则微信拒绝下单
        com.wechat.pay.java.service.payments.h5.model.SceneInfo sceneInfo =
                new com.wechat.pay.java.service.payments.h5.model.SceneInfo();
        sceneInfo.setPayerClientIp(resolveClientIp(request));
        com.wechat.pay.java.service.payments.h5.model.H5Info h5Info =
                new com.wechat.pay.java.service.payments.h5.model.H5Info();
        h5Info.setType(H5_TYPE_WAP);
        sceneInfo.setH5Info(h5Info);
        h5Request.setSceneInfo(sceneInfo);

        com.wechat.pay.java.service.payments.h5.model.PrepayResponse h5Response =
                clients.getH5Service().prepay(h5Request);
        String h5Url = h5Response.getH5Url();

        PayResponse response = successResponse("H5");
        response.setPayUrl(h5Url);
        response.setRawResponse(h5Url);
        return response;
    }

    @Override
    public PayResponse queryOrder(String tradeNo) {
        log.info("[微信支付] 查询订单 tradeNo={}", tradeNo);
        try {
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(config.getWxMchId());
            queryRequest.setOutTradeNo(tradeNo);
            Transaction transaction = clients.getNativePayService().queryOrderByOutTradeNo(queryRequest);

            PayResponse response = new PayResponse();
            Transaction.TradeStateEnum state = transaction.getTradeState();
            response.setRawResponse(state == null ? null : state.name());
            if (state == Transaction.TradeStateEnum.SUCCESS) {
                response.setSuccess(true);
                response.setChannelTradeNo(transaction.getTransactionId());
            } else {
                // 未支付/支付中/已关闭/已撤销/退款等均非成功，不触发支付成功回流
                response.setSuccess(false);
                response.setErrorMsg("微信订单当前状态: " + (state == null ? "UNKNOWN" : state.name()));
            }
            return response;
        } catch (Exception e) {
            log.error("[微信支付] 查询订单失败 tradeNo={}, err={}", tradeNo, errOf(e), e);
            return payFail(errOf(e));
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("[微信支付] 申请退款 transactionId={}, refundAmount={}, outRefundNo={}",
                request.getChannelTradeNo(), request.getAmount(), request.getOutRequestNo());
        RefundResponse response = new RefundResponse();
        if (isBlank(request.getChannelTradeNo())) {
            // 微信退款必须带原支付交易号；真实退款只针对已支付订单，缺失说明前置数据异常
            response.setSuccess(false);
            response.setErrorMsg("缺少微信交易号 transaction_id，无法发起退款");
            return response;
        }
        try {
            // 微信退款金额模型的 total 为「原订单总金额」，按 transaction_id 反查得到
            Long totalFen = queryTotalFen(request.getChannelTradeNo());

            CreateRequest createRequest = new CreateRequest();
            createRequest.setTransactionId(request.getChannelTradeNo());
            createRequest.setOutRefundNo(request.getOutRequestNo());
            if (!isBlank(request.getReason())) {
                createRequest.setReason(request.getReason());
            }
            if (!isBlank(config.getRefundNotifyUrl())) {
                createRequest.setNotifyUrl(config.getRefundNotifyUrl());
            }
            AmountReq amountReq = new AmountReq();
            amountReq.setRefund((long) toFen(request.getAmount()));
            amountReq.setTotal(totalFen);
            amountReq.setCurrency(CURRENCY_CNY);
            createRequest.setAmount(amountReq);

            Refund refund = clients.getRefundService().create(createRequest);
            String status = refund.getStatus() == null ? null : refund.getStatus().name();
            if ("SUCCESS".equals(status)) {
                response.setSuccess(true);
                response.setRefundChannelTradeNo(refund.getRefundId());
            } else if ("PROCESSING".equals(status)) {
                // 退款已受理、待银行/渠道确认：标记 processing，仅登记记录不联动，终态以退款回调为准
                response.setSuccess(false);
                response.setProcessing(true);
                response.setRefundChannelTradeNo(refund.getRefundId());
                response.setErrorMsg("退款处理中（PROCESSING），等待微信确认");
                log.warn("[微信支付] 退款处理中 outRefundNo={}, refundId={}",
                        request.getOutRequestNo(), refund.getRefundId());
            } else {
                response.setSuccess(false);
                response.setErrorMsg("微信退款未成功，状态: " + status);
            }
            return response;
        } catch (Exception e) {
            log.error("[微信支付] 退款失败 transactionId={}, err={}",
                    request.getChannelTradeNo(), errOf(e), e);
            response.setSuccess(false);
            response.setErrorMsg(errOf(e));
            return response;
        }
    }

    /**
     * 按微信交易号反查原订单总金额（分）。
     */
    private Long queryTotalFen(String transactionId) {
        QueryOrderByIdRequest request = new QueryOrderByIdRequest();
        request.setMchid(config.getWxMchId());
        request.setTransactionId(transactionId);
        Transaction transaction = clients.getNativePayService().queryOrderById(request);
        if (transaction.getAmount() == null || transaction.getAmount().getTotal() == null) {
            throw new IllegalStateException("反查原订单金额为空 transactionId=" + transactionId);
        }
        return transaction.getAmount().getTotal().longValue();
    }

    @Override
    public NotifyResult parsePayment(NotifyRequest request) {
        NotifyResult result = new NotifyResult();
        try {
            RequestParam requestParam = buildRequestParam(request);
            NotificationParser parser =
                    new NotificationParser((NotificationConfig) clients.getConfig());
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            result.setTradeNo(transaction.getOutTradeNo());
            result.setChannelTradeNo(transaction.getTransactionId());
            if (transaction.getAmount() != null && transaction.getAmount().getTotal() != null) {
                long totalFen = transaction.getAmount().getTotal().longValue();
                result.setAmount(new BigDecimal(totalFen)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
            if (transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setErrorMsg("微信支付通知状态: " + transaction.getTradeState());
            }
        } catch (Exception e) {
            // 验签失败/解密失败/报文异常均不得触发回流
            log.warn("[微信支付] 支付回调解析失败 err={}", errOf(e));
            result.setSuccess(false);
            result.setErrorMsg("微信支付回调验签/解析失败: " + errOf(e));
        }
        return result;
    }

    @Override
    public RefundNotifyResult parseRefund(NotifyRequest request) {
        RefundNotifyResult result = new RefundNotifyResult();
        try {
            RequestParam requestParam = buildRequestParam(request);
            NotificationParser parser =
                    new NotificationParser((NotificationConfig) clients.getConfig());
            RefundNotification notification = parser.parse(requestParam, RefundNotification.class);

            result.setSuccess(true);
            result.setOutRefundNo(notification.getOutRefundNo());
            result.setRefundId(notification.getRefundId());
            result.setStatus(notification.getRefundStatus() == null
                    ? null : notification.getRefundStatus().name());
        } catch (Exception e) {
            log.warn("[微信支付] 退款回调解析失败 err={}", errOf(e));
            result.setSuccess(false);
            result.setErrorMsg("微信退款回调验签/解析失败: " + errOf(e));
        }
        return result;
    }

    /** 组装微信回调验签所需的头与 body 参数。 */
    private RequestParam buildRequestParam(NotifyRequest request) {
        return new RequestParam.Builder()
                .serialNumber(request.header("Wechatpay-Serial"))
                .nonce(request.header("Wechatpay-Nonce"))
                .signature(request.header("Wechatpay-Signature"))
                .timestamp(request.header("Wechatpay-TimeStamp"))
                .signType(request.header("Wechatpay-Signature-Type"))
                .body(request.getBody())
                .build();
    }

    /**
     * 解析支付方式：仅 NATIVE / H5，默认 NATIVE（PC 也走扫码）。
     */
    private String resolvePayType(String payType) {
        if (payType != null && "H5".equalsIgnoreCase(payType.trim())) {
            return "H5";
        }
        return "NATIVE";
    }

    private String resolveDescription(PayRequest request) {
        if (!isBlank(request.getDescription())) {
            return request.getDescription().trim();
        }
        if (!isBlank(request.getSubject())) {
            return request.getSubject().trim();
        }
        return "瑞吉外卖订单";
    }

    private String resolveClientIp(PayRequest request) {
        if (!isBlank(request.getClientIp())) {
            return request.getClientIp().trim();
        }
        return "127.0.0.1";
    }

    /** 元 → 分（四舍五入到整数分）。 */
    private int toFen(BigDecimal yuan) {
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private PayResponse successResponse(String payType) {
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setPayType(payType);
        return response;
    }

    private PayResponse payFail(String errorMsg) {
        PayResponse response = new PayResponse();
        response.setSuccess(false);
        response.setErrorMsg(errorMsg);
        return response;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String errOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
