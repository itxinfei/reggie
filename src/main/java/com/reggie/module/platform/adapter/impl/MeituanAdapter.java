package com.reggie.module.platform.adapter.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.platform.adapter.PlatformAdapter;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.util.PlatformCredentialEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 美团外卖开放平台适配器（真实对接）
 * <p>
 * 实现美团订单拉取与状态回传。美团开放平台响应通常包裹在 {@code {code,data,...}} 结构中，
 * 订单数据在 data 内；签名与接口路径以美团开放平台文档为准，当前按通用约定实现，
 * 如不一致仅需调整 {@link #buildSign(Map, String)} 与请求路径。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component("platformMeituanAdapter")
public class MeituanAdapter implements PlatformAdapter {

    private static final String PLATFORM_TYPE = "MEITUAN";
    private static final String BASE_URL = "https://openapi.meituan.com/ecommerce/v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MeituanAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String platformType() {
        return PLATFORM_TYPE;
    }

    @Override
    public List<PlatformOrder> pullOrders(PlatformConfig cfg, String beginTime, String endTime) {
        try {
            String accessToken = PlatformCredentialEncryptor.decrypt(cfg.getAccessToken());
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("[美团] accessToken 为空，跳过拉单");
                return Collections.emptyList();
            }
            String url = BASE_URL + "/order/query?accessToken=" + accessToken
                    + "&beginTime=" + beginTime + "&endTime=" + endTime + "&pageSize=100&pageNo=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Meituan-Appkey", cfg.getAppKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseOrders(response.getBody());
            }
            log.warn("[美团] 拉单响应异常: status={}", response.getStatusCode());
        } catch (Exception e) {
            log.error("[美团] 拉单失败", e);
        }
        return Collections.emptyList();
    }

    /**
     * 解析美团订单响应（data 包裹结构），提取标准化订单
     */
    private List<PlatformOrder> parseOrders(String body) throws Exception {
        List<PlatformOrder> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode orderArray = root.path("data").path("orderList");
        if (orderArray.isArray()) {
            for (JsonNode node : orderArray) {
                PlatformOrder po = new PlatformOrder();
                po.setPlatformOrderId(node.path("orderId").asText(""));
                po.setPlatformStatus(node.path("status").asText(""));
                po.setAmount(toDecimal(node.path("total").asText("0")));
                po.setCustomerName(node.path("recipientName").asText(""));
                po.setCustomerPhone(node.path("recipientPhone").asText(""));
                po.setAddress(node.path("address").asText(""));
                po.setRemark(node.path("caution").asText(""));
                po.setOrderTime(node.path("createTime").asText(""));
                po.setItems(parseItems(node.path("detail")));
                po.setRawJson(node.toString());
                result.add(po);
            }
        }
        return result;
    }

    private List<PlatformOrder.OrderItem> parseItems(JsonNode itemsNode) {
        List<PlatformOrder.OrderItem> items = new ArrayList<>();
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                PlatformOrder.OrderItem oi = new PlatformOrder.OrderItem();
                oi.setPlatformDishId(item.path("appFoodCode").asText(""));
                oi.setDishName(item.path("foodName").asText(""));
                oi.setQuantity(item.path("quantity").asInt(1));
                oi.setPrice(toDecimal(item.path("price").asText("0")));
                oi.setFlavor(item.path("spec").asText(""));
                items.add(oi);
            }
        }
        return items;
    }

    @Override
    public void acceptOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/accept",
                Collections.singletonMap("orderId", platformOrderId));
    }

    @Override
    public void rejectOrder(PlatformConfig cfg, String platformOrderId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("orderId", platformOrderId);
        body.put("reason", "商家拒单");
        callPost(cfg, BASE_URL + "/order/reject", body);
    }

    @Override
    public void prepareOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/prepare",
                Collections.singletonMap("orderId", platformOrderId));
    }

    @Override
    public void completeOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/complete",
                Collections.singletonMap("orderId", platformOrderId));
    }

    @Override
    public void cancelOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/cancel",
                Collections.singletonMap("orderId", platformOrderId));
    }

    @Override
    public void syncDishOnShelf(PlatformConfig cfg, Long dishId, String platformDishId) {
        callPost(cfg, BASE_URL + "/sku/up", Collections.singletonMap("skuId", platformDishId));
    }

    @Override
    public void syncDishOffShelf(PlatformConfig cfg, Long dishId, String platformDishId) {
        callPost(cfg, BASE_URL + "/sku/down", Collections.singletonMap("skuId", platformDishId));
    }

    @Override
    public void syncStock(PlatformConfig cfg, String platformDishId, int remainQty) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("skuId", platformDishId);
        body.put("stock", remainQty);
        callPost(cfg, BASE_URL + "/sku/stock", body);
    }

    @Override
    public void syncBusinessStatus(PlatformConfig cfg, boolean open) {
        callPost(cfg, BASE_URL + "/shop/status", Collections.singletonMap("open", open));
    }

    @Override
    public boolean healthCheck(PlatformConfig cfg) {
        try {
            String accessToken = PlatformCredentialEncryptor.decrypt(cfg.getAccessToken());
            if (accessToken == null || accessToken.isEmpty()) {
                return false;
            }
            String url = BASE_URL + "/shop/status?accessToken=" + accessToken;
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            log.error("[美团] 健康检查失败", e);
            return false;
        }
    }

    private void callPost(PlatformConfig cfg, String url, Map<String, Object> body) {
        try {
            String accessToken = PlatformCredentialEncryptor.decrypt(cfg.getAccessToken());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            log.info("[美团] 调用成功: url={}", url);
        } catch (Exception e) {
            log.error("[美团] 调用失败: url={}", url, e);
        }
    }

    private BigDecimal toDecimal(String s) {
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String buildSign(Map<String, Object> params, String secret) {
        // 通用约定：参数按 key 字典序拼接 k=v&key=secret 后 MD5 大写，与实际平台文档不一致时替换
        java.util.TreeMap<String, Object> sorted = new java.util.TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (e.getValue() == null || e.getValue().toString().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        sb.append("&key=").append(secret);
        return md5Upper(sb.toString());
    }

    private String md5Upper(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }
}
