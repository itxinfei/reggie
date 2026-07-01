package com.reggie.module.delivery.platform;

import java.util.List;
import java.util.Map;

public interface DeliveryPlatform {
    boolean acceptOrder(String platformOrderId);
    boolean syncMenu(List<Map<String, Object>> dishes);
    boolean updateStatus(String platformOrderId, String status);
    boolean syncStock(Map<Long, Integer> stock);
}
