package com.reggie.module.payment.channel.notify;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付/退款回调请求载体：原始 body + 全部请求头。
 * <p>
 * 微信 APIv3 验签需要 Wechatpay-Signature/Nonce/TimeStamp/Serial 等头，
 * 故控制器读取原始 body 后连同头一并封装交给解析器，而不是只传 Map。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
public class NotifyRequest {

    private String body;
    private Map<String, String> headers = new HashMap<>();

    /** 按名称取头，大小写不敏感（HTTP 头名不区分大小写）。 */
    public String header(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 从 HTTP 请求构建：读取好的 body + 拷贝全部头。 */
    public static NotifyRequest from(HttpServletRequest request, String body) {
        NotifyRequest notifyRequest = new NotifyRequest();
        notifyRequest.body = body;
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headerMap.put(name, request.getHeader(name));
            }
        }
        notifyRequest.headers = headerMap;
        return notifyRequest;
    }
}
