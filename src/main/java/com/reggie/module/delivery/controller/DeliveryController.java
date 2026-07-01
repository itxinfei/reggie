package com.reggie.module.delivery.controller;

import com.reggie.common.R;
import com.reggie.module.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/delivery")
@Tag(name = "外卖平台对接")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @PostMapping("/accept")
    public R<String> acceptOrder(@RequestBody Map<String, String> params) {
        boolean result = deliveryService.acceptOrder(params.get("platform"), params.get("platformOrderId"));
        return result ? R.success("接单成功") : R.error("接单失败");
    }

    @PostMapping("/sync/menu")
    public R<String> syncMenu(@RequestBody Map<String, Object> params) {
        String platform = (String) params.get("platform");
        List<Map<String, Object>> dishes = (List<Map<String, Object>>) params.get("dishes");
        boolean result = deliveryService.syncMenu(platform, dishes);
        return result ? R.success("菜单同步成功") : R.error("菜单同步失败");
    }

    @PostMapping("/sync/stock")
    public R<String> syncStock(@RequestBody Map<String, Object> params) {
        String platform = (String) params.get("platform");
        Map<String, Object> stockRaw = (Map<String, Object>) params.get("stock");
        Map<Long, Integer> stock = new HashMap<>();
        for (Map.Entry<String, Object> e : stockRaw.entrySet()) {
            stock.put(Long.valueOf(e.getKey()), Integer.valueOf(e.getValue().toString()));
        }
        boolean result = deliveryService.syncStock(platform, stock);
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    @PostMapping("/callback/{platform}")
    public R<String> callback(@PathVariable String platform, @RequestBody Map<String, String> params) {
        String result = deliveryService.handleCallback(platform, params);
        return R.success(result);
    }
}
