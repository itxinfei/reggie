package com.reggie.module.delivery.platform;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 配送平台接口（策略模式），定义与第三方配送平台的交互规范。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public interface DeliveryPlatform {
    /**
     * 接受订单
     *
     * @param platformOrderId 平台订单号
     * @return 是否接受成功
     */
    boolean acceptOrder(String platformOrderId);
    /**
     * 同步菜单到平台
     *
     * @param dishes 菜品列表
     * @return 是否同步成功
     */
    boolean syncMenu(List<Map<String, Object>> dishes);
    /**
     * 更新订单状态
     *
     * @param platformOrderId 平台订单号
     * @param status          新状态
     * @return 是否更新成功
     */
    boolean updateStatus(String platformOrderId, String status);
    /**
     * 同步库存到平台
     *
     * @param stock 库存映射（菜品ID -> 数量）
     * @return 是否同步成功
     */
    boolean syncStock(Map<Long, Integer> stock);

    /**
     * 校验平台回调签名（防伪造回调）。
     * <p>
     * 回调是外部请求无登录态，必须通过签名校验防止伪造。
     * <b>默认 fail-closed（返回 false）</b>：未实现验签的平台一律拒绝回调，
     * 严禁恒真放行。具体平台应继承 {@link AbstractDeliveryPlatform}，
     * 由其按 mock-mode / 凭证配置统一处理。
     * </p>
     *
     * @param params 回调参数（含签名、时间戳等）
     * @return true=签名合法，false=签名不匹配
     */
    default boolean verifyCallback(Map<String, String> params) {
        return false;
    }
}
