package com.reggie.module.delivery.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ElemeAdapter implements DeliveryPlatform {

    @Override
    public boolean acceptOrder(String platformOrderId) {
        log.info("[饿了么] 自动接单: {}", platformOrderId);
        return true;
    }

    @Override
    public boolean syncMenu(List<Map<String, Object>> dishes) {
        log.info("[饿了么] 同步菜单: {} 个菜品", dishes.size());
        return true;
    }

    @Override
    public boolean updateStatus(String platformOrderId, String status) {
        log.info("[饿了么] 更新订单状态: {} -> {}", platformOrderId, status);
        return true;
    }

    @Override
    public boolean syncStock(Map<Long, Integer> stock) {
        log.info("[饿了么] 同步库存: {} 个商品", stock.size());
        return true;
    }
}
