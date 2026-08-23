package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.config.DeliveryPlatformConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 抖音外卖平台适配器。
 * </p>
 *
 * <p>
 * 对接方式：mock-mode=true（默认，开发/演示）跳过真实调用仅告警；
 * 生产环境需在 yml 配置 {@code reggie.delivery.platforms.DOUYIN.enabled=true} 及
 * appId/appSecret/baseUrl/merchantId/notifyToken 后才会发起真实 HTTP 调用；
 * 未配置时平台操作 fail-closed（返回 false），不再静默假成功。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-11
 */
@Slf4j
@Component
public class DouyinAdapter extends AbstractDeliveryPlatform {

    /** 平台标识 */
    private static final String KEY = "DOUYIN";

    @Override
    protected String platformKey() {
        return KEY;
    }

    /**
     * 接受订单（抖音开放平台接单接口）
     *
     * @param platformOrderId 平台订单号
     * @return 是否接受成功
     */
    @Override
    public boolean acceptOrder(String platformOrderId) {
        if (isMockOrUnconfigured("自动接单")) {
            return config.isMockMode();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", platformOrderId);
        String body = postPlatform("order/accept", params);
        return body != null;
    }

    /**
     * 同步菜品到平台（全量同步）
     *
     * @param dishes 菜品列表
     * @return 是否同步成功
     */
    @Override
    public boolean syncMenu(List<Map<String, Object>> dishes) {
        if (isMockOrUnconfigured("同步菜单")) {
            return config.isMockMode();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("dishes", dishes);
        String body = postPlatform("menu/sync", params);
        return body != null;
    }

    /**
     * 更新订单状态
     *
     * @param platformOrderId 平台订单号
     * @param status          新状态
     * @return 是否更新成功
     */
    @Override
    public boolean updateStatus(String platformOrderId, String status) {
        if (isMockOrUnconfigured("更新订单状态")) {
            return config.isMockMode();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", platformOrderId);
        params.put("status", status);
        String body = postPlatform("order/status/update", params);
        return body != null;
    }

    /**
     * 同步库存到平台
     *
     * @param stock 库存映射（菜品ID -> 数量）
     * @return 是否同步成功
     */
    @Override
    public boolean syncStock(Map<Long, Integer> stock) {
        if (isMockOrUnconfigured("同步库存")) {
            return config.isMockMode();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("stock", stock);
        String body = postPlatform("stock/sync", params);
        return body != null;
    }

    /**
     * 校验抖音回调签名。
     * mock-mode 跳过验签；生产模式使用 notifyToken 做通用签名校验（MD5 大写比对），
     * 未配置 notifyToken 时 fail-closed 拒绝。
     *
     * @param params 回调参数
     * @return true=签名合法；false=校验失败
     */
    @Override
    public boolean verifyCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        if (config.isMockMode()) {
            log.warn("[抖音] 回调签名校验已跳过（mock-mode=true，仅限开发/演示），生产环境必须关闭 mock-mode 并配置 notify-token");
            return true;
        }
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        if (pc == null || isBlank(pc.getNotifyToken())) {
            log.error("[抖音] 回调签名校验失败：平台未配置 notify-token（fail-closed）");
            return false;
        }
        String sign = params.get("sign");
        if (sign == null || sign.trim().isEmpty()) {
            log.warn("[抖音] 回调缺少 sign 参数");
            return false;
        }
        java.util.TreeMap<String, Object> sort = new java.util.TreeMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!"sign".equals(e.getKey()) && e.getValue() != null && !e.getValue().trim().isEmpty()) {
                sort.put(e.getKey(), e.getValue());
            }
        }
        String computed = md5Upper(buildSignContent(sort) + "&key=" + pc.getNotifyToken());
        boolean ok = computed.equals(sign.trim().toUpperCase());
        if (!ok) {
            log.warn("[抖音] 回调签名校验失败（签名不匹配）");
        }
        return ok;
    }

    private String buildSignContent(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
