package com.reggie.module.delivery.service.impl;

import com.reggie.module.delivery.platform.DeliveryPlatform;
import com.reggie.module.delivery.platform.DeliveryPlatformFactory;
import com.reggie.module.delivery.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    @Autowired
    private DeliveryPlatformFactory factory;

    @Override
    public boolean acceptOrder(String platform, String platformOrderId) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;
        return dp.acceptOrder(platformOrderId);
    }

    @Override
    public boolean syncMenu(String platform, List<Map<String, Object>> dishes) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;
        return dp.syncMenu(dishes);
    }

    @Override
    public boolean syncStock(String platform, Map<Long, Integer> stock) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return false;
        return dp.syncStock(stock);
    }

    @Override
    public String handleCallback(String platform, Map<String, String> params) {
        DeliveryPlatform dp = factory.getPlatform(platform);
        if (dp == null) return "error: unknown platform";
        log.info("Received callback from {}: {}", platform, params);
        return "success";
    }
}
