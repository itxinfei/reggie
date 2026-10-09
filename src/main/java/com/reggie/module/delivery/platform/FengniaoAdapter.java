package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.config.DeliveryPlatformConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 蜂鸟配送平台适配器（mock 模式，仅打印日志）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Slf4j
@Component
public class FengniaoAdapter extends AbstractDeliveryPlatform {

    private static final String KEY = "FENGNIAO";

    /**
     * 处理 platform key。
     * @return 返回结果
     */
    @Override
    protected String platformKey() {
        return KEY;
    }

    /**
     * 接单 order。
     * @param platformOrderId 参数 platformOrderId
     * @return 返回结果
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
     * 同步 menu。
     * @param dishes 参数 dishes
     * @return 返回结果
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
     * 更新 status。
     * @param platformOrderId 参数 platformOrderId
     * @param status 参数 status
     * @return 返回结果
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
     * 同步 stock。
     * @param stock 参数 stock
     * @return 返回结果
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
     * 校验 callback。
     * @param params 参数 params
     * @return 返回结果
     */
    @Override
    public boolean verifyCallback(Map<String, String> params) {
        DeliveryPlatformConfigProperties.PlatformConfig pc = getPlatformConfig();
        String token = pc == null ? null : pc.getNotifyToken();
        return verifyCallbackByToken(params, token, "蜂鸟");
    }
}
