package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.config.DeliveryPlatformConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 顺丰配送平台适配器（mock 模式，仅打印日志）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Slf4j
@Component
public class ShunfengAdapter extends AbstractDeliveryPlatform {

    private static final String KEY = "SHUNFENG";

    @Override
    protected String platformKey() {
        return KEY;
    }

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

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        String token = pc == null ? null : pc.getNotifyToken();
        return verifyCallbackByToken(params, token, "顺丰");
    }
}
