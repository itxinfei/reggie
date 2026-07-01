package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.DeliveryOrder;
import java.util.List;
import java.util.Map;

public interface DeliveryService {
    DeliveryOrder getById(String id);
    Page<DeliveryOrder> pageOrders(int page, int pageSize, String platform, String status, String startDate, String endDate);
    boolean acceptOrder(String platform, String platformOrderId);
    boolean syncMenu(String platform, List<Map<String, Object>> dishes);
    boolean syncStock(String platform, Map<Long, Integer> stock);
    String handleCallback(String platform, Map<String, String> params);
}
