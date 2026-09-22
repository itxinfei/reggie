package com.reggie.module.payment.channel.notify;

import javax.servlet.http.HttpServletRequest;

/**
 * 回调地址的租户路由工具。
 * <p>
 * <b>为什么需要它：</b>微信 APIv3 支付回调的业务字段（out_trade_no、mchid）全部位于 AEAD 加密密文中，
 * 外层 JSON 与请求头不含任何商户/租户标识，回调进来时无法在「选对租户密钥」之前解密路由；
 * 支付宝回调虽含明文 out_trade_no，但为两渠道路由统一，均采用在 notify_url 上追加 {@code tenantId} 参数。
 * 该参数仅用于选择对应租户的配置/密钥（路由），不参与任何信任决策——
 * 即便被篡改，随后用错误租户密钥验签必然失败，回调会被拒绝。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public final class NotifyUrlRouter {

    private static final String TENANT_PARAM = "tenantId";

    private NotifyUrlRouter() {
    }

    /**
     * 在回调地址上追加租户参数（下单时调用）。
     */
    public static String withTenant(String notifyUrl, Long tenantId) {
        if (notifyUrl == null || notifyUrl.trim().isEmpty() || tenantId == null) {
            return notifyUrl;
        }
        String separator = notifyUrl.indexOf('?') >= 0 ? "&" : "?";
        return notifyUrl + separator + TENANT_PARAM + "=" + tenantId;
    }

    /**
     * 从回调请求读取租户参数（query string）。非法/缺失返回 null。
     */
    public static Long readTenant(HttpServletRequest request) {
        String value = request.getParameter(TENANT_PARAM);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
