package com.reggie.module.delivery.service;

import java.util.List;
import java.util.Map;

public interface DeliveryService {
    boolean acceptOrder(String platform, String platformOrderId);
    boolean syncMenu(String platform, List<Map<String, Object>> dishes);
    boolean syncStock(String platform, Map<Long, Integer> stock);
    String handleCallback(String platform, Map<String, String> params);
}
