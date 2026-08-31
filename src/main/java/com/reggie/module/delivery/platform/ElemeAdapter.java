package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.config.DeliveryPlatformConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 饿了么外卖平台适配器。
 * </p>
 *
 * <p>
 * 对接方式：mock-mode=true（默认，开发/演示）跳过真实调用仅告警；
 * 生产环境需在 yml 配置 {@code reggie.delivery.platforms.ELEME.enabled=true} 及
 * appId/appSecret/baseUrl/merchantId/notifyToken 后才会发起真实 HTTP 调用；
 * 未配置时平台操作 fail-closed（返回 false），不再静默假成功。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class ElemeAdapter extends AbstractDeliveryPlatform {

    /** 平台标识 */
    private static final String KEY = "ELEME";

    @Override
    protected String platformKey() {
        return KEY;
    }

    /**
     * 接受订单（饿了么开放平台接单接口）
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
     * 同步菜单到平台
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
     * 校验饿了么回调签名（按饿了么开放平台官方签名规范实现）。
     *
     * <p>饿了么官方签名规则：除 sign 外非空参数按 key 字典序升序拼接为 {@code k1=v1&k2=v2...}，
     * 末尾拼接 {@code &key=notifyToken}，整体 MD5 转大写，与回调 sign 大写比对。
     * 同时校验 timestamp 时效性防重放（详见基类 {@link #verifyCallbackByToken}）。</p>
     *
     * @param params 回调参数
     * @return true=签名合法；false=校验失败
     */
    @Override
    public boolean verifyCallback(Map<String, String> params) {
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        String token = pc == null ? null : pc.getNotifyToken();
        return verifyCallbackByToken(params, token, "饿了么");
    }
}
