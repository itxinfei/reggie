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
import java.lang.NumberFormatException;

/**
 * 京东外卖（京东到家开放平台）适配器（真实对接）
 * <p>
 * 实现京东外卖订单拉取与状态回传。京东到家开放平台响应通常包裹在
 * {@code {code,msg,result:[...]}} 结构中，订单数组直接位于 result 内，
 * 字段以驼峰（camelCase）命名；签名与接口路径以京东到家开放平台文档为准，
 * 当前按通用约定实现，如不一致仅需调整 {@link #buildSign(Map, String)} 与请求路径。
 * </p>
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@Component("platformJdAdapter")
public class JdAdapter implements PlatformAdapter {

    private static final String PLATFORM_TYPE = "JD";
    private static final String BASE_URL = "https://openo2o.jddj.com/djapi/v1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JdAdapter() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 处理 platform type。
     * @return 返回结果
     */
    @Override
    public String platformType() {
        return PLATFORM_TYPE;
    }

    /**
     * 拉取 orders。
     * @param cfg 参数 cfg
     * @param beginTime 参数 beginTime
     * @param endTime 参数 endTime
     * @return 返回结果
     */
    @Override
    public List<PlatformOrder> pullOrders(PlatformConfig cfg, String beginTime, String endTime) {
        try {
            String accessToken = PlatformCredentialEncryptor.decrypt(cfg.getAccessToken());
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("[京东] accessToken 为空，跳过拉单");
                return Collections.emptyList();
            }
            String url = BASE_URL + "/order/pull?access_token=" + accessToken
                    + "&beginTime=" + beginTime + "&endTime=" + endTime + "&pageSize=100&pageNo=1";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Jd-AppKey", cfg.getAppKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseOrders(response.getBody());
            }
            log.warn("[京东] 拉单响应异常: status={}", response.getStatusCode());
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[京东] 拉单失败", e);
        }
        return Collections.emptyList();
    }

    /**
     * 解析京东外卖订单响应（code/result 数组包裹结构，camelCase），提取标准化订单
     */
    private List<PlatformOrder> parseOrders(String body) throws Exception {
        List<PlatformOrder> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode orderArray = root.path("result");
        if (orderArray.isArray()) {
            for (JsonNode node : orderArray) {
                PlatformOrder po = new PlatformOrder();
                po.setPlatformOrderId(node.path("orderId").asText(""));
                po.setPlatformStatus(node.path("status").asText(""));
                po.setAmount(toDecimal(node.path("totalFee").asText("0")));
                po.setCustomerName(node.path("consigneeName").asText(""));
                po.setCustomerPhone(node.path("consigneePhone").asText(""));
                po.setAddress(node.path("address").asText(""));
                po.setRemark(node.path("remark").asText(""));
                po.setOrderTime(node.path("createTime").asText(""));
                po.setItems(parseItems(node.path("skuList")));
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
                oi.setPlatformDishId(item.path("skuId").asText(""));
                oi.setDishName(item.path("skuName").asText(""));
                oi.setQuantity(item.path("skuNum").asInt(1));
                oi.setPrice(toDecimal(item.path("skuPrice").asText("0")));
                oi.setFlavor(item.path("skuSpec").asText(""));
                items.add(oi);
            }
        }
        return items;
    }

    /**
     * 接单 order。
     * @param cfg 参数 cfg
     * @param platformOrderId 参数 platformOrderId
     */
    @Override
    public void acceptOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/accept",
                Collections.singletonMap("orderId", platformOrderId));
    }

    /**
     * 驳回 order。
     * @param cfg 参数 cfg
     * @param platformOrderId 参数 platformOrderId
     */
    @Override
    public void rejectOrder(PlatformConfig cfg, String platformOrderId) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("orderId", platformOrderId);
        body.put("cancelReason", "商家拒单");
        callPost(cfg, BASE_URL + "/order/reject", body);
    }

    /**
     * 处理 prepare order。
     * @param cfg 参数 cfg
     * @param platformOrderId 参数 platformOrderId
     */
    @Override
    public void prepareOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/ready",
                Collections.singletonMap("orderId", platformOrderId));
    }

    /**
     * 完成 order。
     * @param cfg 参数 cfg
     * @param platformOrderId 参数 platformOrderId
     */
    @Override
    public void completeOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/finish",
                Collections.singletonMap("orderId", platformOrderId));
    }

    /**
     * 取消 order。
     * @param cfg 参数 cfg
     * @param platformOrderId 参数 platformOrderId
     */
    @Override
    public void cancelOrder(PlatformConfig cfg, String platformOrderId) {
        callPost(cfg, BASE_URL + "/order/cancel",
                Collections.singletonMap("orderId", platformOrderId));
    }

    /**
     * 同步 dish on shelf。
     * @param cfg 参数 cfg
     * @param dishId 参数 dishId
     * @param platformDishId 参数 platformDishId
     */
    @Override
    public void syncDishOnShelf(PlatformConfig cfg, Long dishId, String platformDishId) {
        callPost(cfg, BASE_URL + "/sku/up", Collections.singletonMap("skuId", platformDishId));
    }

    /**
     * 同步 dish off shelf。
     * @param cfg 参数 cfg
     * @param dishId 参数 dishId
     * @param platformDishId 参数 platformDishId
     */
    @Override
    public void syncDishOffShelf(PlatformConfig cfg, Long dishId, String platformDishId) {
        callPost(cfg, BASE_URL + "/sku/down", Collections.singletonMap("skuId", platformDishId));
    }

    /**
     * 同步 stock。
     * @param cfg 参数 cfg
     * @param platformDishId 参数 platformDishId
     * @param remainQty 参数 remainQty
     */
    @Override
    public void syncStock(PlatformConfig cfg, String platformDishId, int remainQty) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("skuId", platformDishId);
        body.put("remainStock", remainQty);
        callPost(cfg, BASE_URL + "/sku/stock", body);
    }

    /**
     * 同步 business status。
     * @param cfg 参数 cfg
     * @param open 参数 open
     */
    @Override
    public void syncBusinessStatus(PlatformConfig cfg, boolean open) {
        callPost(cfg, BASE_URL + "/shop/businessStatus", Collections.singletonMap("open", open));
    }

    /**
     * 处理 health check。
     * @param cfg 参数 cfg
     * @return 返回结果
     */
    @Override
    public boolean healthCheck(PlatformConfig cfg) {
        try {
            String accessToken = PlatformCredentialEncryptor.decrypt(cfg.getAccessToken());
            if (accessToken == null || accessToken.isEmpty()) {
                return false;
            }
            String url = BASE_URL + "/shop/businessStatus?access_token=" + accessToken;
            restTemplate.getForObject(url, String.class);
            return true;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[京东] 健康检查失败", e);
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
            log.info("[京东] 调用成功: url={}", url);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[京东] 调用失败: url={}", url, e);
        }
    }

    private BigDecimal toDecimal(String s) {
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }
}
