package com.reggie.module.payment.channel.real;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradePrecreateModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
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
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.utils.QRCodeUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝真实渠道。
 * <p>
 * 通过官方 {@link AlipayClient} 实现：手机网站支付（WAP 跳转）、扫码支付（预下单生成二维码）、
 * 电脑网站支付、按商户单号查询、申请退款。接口同步返回，{@code code=10000} 为确定成功。
 * 实例由 {@code PaymentChannelFactory} 在非 mock 模式下按「租户 + 渠道」创建并缓存，非 Spring Bean。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
public class RealAlipayChannel implements PaymentChannel, RealNotifyCapable {

    private static final String CODE_SUCCESS = "10000";
    private static final String WAP_PRODUCT_CODE = "QUICK_WAP_WAY";
    private static final String PAGE_PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";
    private static final String HTTP_GET = "GET";

    private final AlipayClient alipayClient;
    private final PaymentChannelConfig config;
    private final QRCodeUtil qrCodeUtil;

    public RealAlipayChannel(AlipayClient alipayClient, PaymentChannelConfig config,
                             QRCodeUtil qrCodeUtil) {
        this.alipayClient = alipayClient;
        this.config = config;
        this.qrCodeUtil = qrCodeUtil;
    }

    @Override
    public PayResponse createOrder(PayRequest request) {
        String scene = resolveScene(request.getPayType());
        log.info("[支付宝] 创建订单 tradeNo={}, amount={}, scene={}",
                request.getTradeNo(), request.getAmount(), scene);
        try {
            if ("NATIVE".equals(scene)) {
                return precreate(request);
            }
            if ("PC".equals(scene)) {
                return pagePay(request);
            }
            return wapPay(request);
        } catch (Exception e) {
            log.error("[支付宝] 下单失败 tradeNo={}, err={}", request.getTradeNo(), errOf(e), e);
            return payFail(errOf(e));
        }
    }

    /**
     * 手机网站支付：pageExecute(GET) 返回可直接跳转的支付宝收银台 URL。
     */
    private PayResponse wapPay(PayRequest request) throws Exception {
        AlipayTradeWapPayRequest payRequest = new AlipayTradeWapPayRequest();
        payRequest.setNotifyUrl(NotifyUrlRouter.withTenant(config.getPayNotifyUrl(), config.getTenantId()));

        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(request.getTradeNo());
        model.setSubject(resolveSubject(request));
        model.setTotalAmount(formatYuan(request.getAmount()));
        model.setProductCode(WAP_PRODUCT_CODE);
        payRequest.setBizModel(model);

        AlipayTradeWapPayResponse response = alipayClient.pageExecute(payRequest, HTTP_GET);
        String payUrl = response.getBody();

        PayResponse result = successResponse("H5");
        result.setPayUrl(payUrl);
        result.setRawResponse(payUrl);
        return result;
    }

    /**
     * 扫码支付：预下单返回 qrCode，渲染为 PNG 二维码 Data URI。
     */
    private PayResponse precreate(PayRequest request) throws Exception {
        AlipayTradePrecreateRequest payRequest = new AlipayTradePrecreateRequest();
        payRequest.setNotifyUrl(NotifyUrlRouter.withTenant(config.getPayNotifyUrl(), config.getTenantId()));

        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setOutTradeNo(request.getTradeNo());
        model.setSubject(resolveSubject(request));
        model.setTotalAmount(formatYuan(request.getAmount()));
        payRequest.setBizModel(model);

        AlipayTradePrecreateResponse response = alipayClient.execute(payRequest);
        if (!CODE_SUCCESS.equals(response.getCode())) {
            log.warn("[支付宝] 预下单失败 tradeNo={}, {}", request.getTradeNo(), alipayError(response));
            return payFail(alipayError(response));
        }
        String qrCode = response.getQrCode();
        PayResponse result = successResponse("NATIVE");
        result.setPayUrl(qrCode);
        result.setQrCodeUrl(qrCodeUtil.generateDataUri(qrCode));
        result.setRawResponse(qrCode);
        return result;
    }

    /**
     * 电脑网站支付：pageExecute(GET) 返回可跳转的电脑收银台 URL。
     */
    private PayResponse pagePay(PayRequest request) throws Exception {
        AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
        payRequest.setNotifyUrl(NotifyUrlRouter.withTenant(config.getPayNotifyUrl(), config.getTenantId()));

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(request.getTradeNo());
        model.setSubject(resolveSubject(request));
        model.setTotalAmount(formatYuan(request.getAmount()));
        model.setProductCode(PAGE_PRODUCT_CODE);
        payRequest.setBizModel(model);

        AlipayResponse response = alipayClient.pageExecute(payRequest, HTTP_GET);
        String payUrl = response.getBody();

        PayResponse result = successResponse("PC");
        result.setPayUrl(payUrl);
        result.setRawResponse(payUrl);
        return result;
    }

    @Override
    public PayResponse queryOrder(String tradeNo) {
        log.info("[支付宝] 查询订单 tradeNo={}", tradeNo);
        try {
            AlipayTradeQueryRequest queryRequest = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(tradeNo);
            queryRequest.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(queryRequest);
            PayResponse result = new PayResponse();
            if (CODE_SUCCESS.equals(response.getCode())) {
                String tradeStatus = response.getTradeStatus();
                result.setRawResponse(tradeStatus);
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    result.setSuccess(true);
                    result.setChannelTradeNo(response.getTradeNo());
                } else {
                    // WAIT_BUYER_PAY（待付款）/ TRADE_CLOSED（已关闭）均非成功
                    result.setSuccess(false);
                    result.setErrorMsg("支付宝订单当前状态: " + tradeStatus);
                }
            } else {
                // 订单不存在等业务错误码（如 ACQ.TRADE_NOT_EXIST）视为未支付
                result.setSuccess(false);
                result.setErrorMsg(alipayError(response));
            }
            return result;
        } catch (Exception e) {
            log.error("[支付宝] 查询订单失败 tradeNo={}, err={}", tradeNo, errOf(e), e);
            return payFail(errOf(e));
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("[支付宝] 申请退款 tradeNo={}, refundAmount={}, outRequestNo={}",
                request.getChannelTradeNo(), request.getAmount(), request.getOutRequestNo());
        RefundResponse response = new RefundResponse();
        if (isBlank(request.getChannelTradeNo())) {
            response.setSuccess(false);
            response.setErrorMsg("缺少支付宝交易号，无法发起退款");
            return response;
        }
        try {
            AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            // channelTradeNo 存的是支付宝回调 trade_no（支付宝流水号）
            model.setTradeNo(request.getChannelTradeNo());
            model.setRefundAmount(formatYuan(request.getAmount()));
            model.setOutRequestNo(request.getOutRequestNo());
            if (!isBlank(request.getReason())) {
                model.setRefundReason(request.getReason());
            }
            refundRequest.setBizModel(model);

            AlipayTradeRefundResponse alipayResponse = alipayClient.execute(refundRequest);
            if (CODE_SUCCESS.equals(alipayResponse.getCode())) {
                // 支付宝退款同步确定成功，无 processing 中间态
                response.setSuccess(true);
                response.setRefundChannelTradeNo(request.getChannelTradeNo());
            } else {
                response.setSuccess(false);
                response.setErrorMsg(alipayError(alipayResponse));
            }
            return response;
        } catch (Exception e) {
            log.error("[支付宝] 退款失败 tradeNo={}, err={}", request.getChannelTradeNo(), errOf(e), e);
            response.setSuccess(false);
            response.setErrorMsg(errOf(e));
            return response;
        }
    }

    @Override
    public NotifyResult parsePayment(NotifyRequest request) {
        NotifyResult result = new NotifyResult();
        try {
            Map<String, String> params = parseForm(request.getBody());
            // 真实 RSA2 验签，公钥用库内配置（兼容纯 Base64 / 带 PEM 头尾）
            boolean signOk = AlipaySignature.rsaCheckV1(params,
                    stripKeyPem(config.getAliPublicKey()), "UTF-8", "RSA2");
            if (!signOk) {
                result.setSuccess(false);
                result.setErrorMsg("支付宝回调验签失败");
                return result;
            }
            result.setTradeNo(params.get("out_trade_no"));
            result.setChannelTradeNo(params.get("trade_no"));
            String totalAmount = params.get("total_amount");
            if (totalAmount != null && !totalAmount.trim().isEmpty()) {
                result.setAmount(new BigDecimal(totalAmount));
            }
            String tradeStatus = params.get("trade_status");
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                result.setSuccess(true);
            } else {
                // WAIT_BUYER_PAY 等非成功状态不触发回流
                result.setSuccess(false);
                result.setErrorMsg("支付宝回调状态: " + tradeStatus);
            }
        } catch (Exception e) {
            log.warn("[支付宝] 支付回调解析失败 err={}", errOf(e));
            result.setSuccess(false);
            result.setErrorMsg("支付宝回调验签/解析失败: " + errOf(e));
        }
        return result;
    }

    @Override
    public RefundNotifyResult parseRefund(NotifyRequest request) {
        // 支付宝退款为同步确定结果（execute 即返回最终状态），平台不推送退款异步通知
        RefundNotifyResult result = new RefundNotifyResult();
        result.setSuccess(false);
        result.setErrorMsg("支付宝无退款异步回调，退款结果以同步响应为准");
        return result;
    }

    /**
     * 解析 application/x-www-form-urlencoded 的回调 body 为 Map（URL 解码）。
     * 不依赖 request.getParameterMap：控制器已读取原始 body，二者不能同时消费。
     */
    private Map<String, String> parseForm(String body) throws Exception {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            String rawKey = index >= 0 ? pair.substring(0, index) : pair;
            String rawValue = index >= 0 ? pair.substring(index + 1) : "";
            map.put(URLDecoder.decode(rawKey, "UTF-8"), URLDecoder.decode(rawValue, "UTF-8"));
        }
        return map;
    }

    /** 去除 PEM 头尾与空白，得到纯 Base64（与 AlipaySdkBuilder 一致）。 */
    private String stripKeyPem(String key) {
        if (key == null) {
            return "";
        }
        String content = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
        return content.replaceAll("\\s+", "");
    }

    /**
     * 解析场景：WAP（默认，C 端移动端）/ NATIVE（扫码）/ PC（电脑网页）。
     */
    private String resolveScene(String payType) {
        if (payType == null) {
            return "WAP";
        }
        String t = payType.trim().toUpperCase();
        if ("NATIVE".equals(t)) {
            return "NATIVE";
        }
        if ("PC".equals(t)) {
            return "PC";
        }
        return "WAP";
    }

    private String resolveSubject(PayRequest request) {
        if (!isBlank(request.getDescription())) {
            return request.getDescription().trim();
        }
        if (!isBlank(request.getSubject())) {
            return request.getSubject().trim();
        }
        return "瑞吉外卖订单";
    }

    /** 金额格式化为支付宝要求的「元」字符串（保留 2 位小数）。 */
    private String formatYuan(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String alipayError(AlipayResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("code=").append(response.getCode());
        if (!isBlank(response.getSubCode())) {
            sb.append(", subCode=").append(response.getSubCode());
        }
        if (!isBlank(response.getSubMsg())) {
            sb.append(", subMsg=").append(response.getSubMsg());
        }
        return sb.toString();
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
