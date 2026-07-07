package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/orders")
    @Operation(summary = "分页查询外卖订单")
    public R<Page<DeliveryOrder>> pageOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<DeliveryOrder> pageInfo = deliveryService.pageOrders(page, pageSize, platform, status, startDate, endDate);
        return R.success(pageInfo);
    }

    @PostMapping("/accept")
    @Operation(summary = "接单")
    public R<String> acceptOrder(@RequestBody Map<String, String> params) {
        String platform = params.get("platform");
        String platformOrderId = params.get("platformOrderId");

        // 如果都没有提供，尝试通过orderId查询
        if (platform == null && platformOrderId == null) {
            String orderId = params.get("orderId");
            if (orderId == null) {
                return R.error("接单失败");
            }
            DeliveryOrder order = deliveryService.getById(orderId);
            if (order == null) {
                return R.error("接单失败");
            }
            platform = order.getPlatform();
            platformOrderId = order.getPlatformOrderId();
        }

        // 校验必要参数
        if (platform == null || platformOrderId == null) {
            return R.error("接单失败");
        }

        boolean result = deliveryService.acceptOrder(platform, platformOrderId);
        return result ? R.success("接单成功") : R.error("接单失败");
    }

    @PostMapping("/sync/menu")
    @Operation(summary = "同步菜品")
    public R<String> syncMenu(@RequestBody Map<String, Object> params) {
        String platform = (String) params.get("platform");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dishes = (List<Map<String, Object>>) params.get("dishes");
        boolean result = deliveryService.syncMenu(platform, dishes);
        return result ? R.success("菜单同步成功") : R.error("菜单同步失败");
    }

    @PostMapping("/sync/stock")
    @Operation(summary = "同步库存")
    public R<String> syncStock(@RequestBody Map<String, Object> params) {
        String platform = (String) params.get("platform");
        @SuppressWarnings("unchecked")
        Map<String, Object> stockRaw = (Map<String, Object>) params.get("stock");
        Map<Long, Integer> stock = new HashMap<>();
        for (Map.Entry<String, Object> e : stockRaw.entrySet()) {
            try {
                stock.put(Long.valueOf(e.getKey()), Integer.valueOf(e.getValue().toString()));
            } catch (NumberFormatException ex) {
                log.warn("库存数据格式错误: key={}, value={}", e.getKey(), e.getValue());
                return R.error("库存数据格式错误");
            }
        }
        boolean result = deliveryService.syncStock(platform, stock);
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    @PostMapping("/callback/{platform}")
    @Operation(summary = "平台回调")
    public R<String> callback(@PathVariable String platform, @RequestBody Map<String, String> params) {
        String result = deliveryService.handleCallback(platform, params);
        return R.success(result);
    }
}
