package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.AcceptOrderDTO;
import com.reggie.dto.SyncMenuDTO;
import com.reggie.dto.SyncStockDTO;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * 外卖平台对接控制器
 * 提供外卖订单管理、菜品同步、库存同步等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery")
@Tag(name = "外卖平台对接")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    @GetMapping("/orders")
    @Operation(summary = "分页查询外卖订单", description = "分页查询外卖平台订单，支持按平台、状态、时间范围筛选")
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
    @Operation(summary = "接单", description = "确认接单外卖订单")
    public R<String> acceptOrder(@Valid @RequestBody AcceptOrderDTO dto) {
        boolean result = deliveryService.acceptOrder(dto.getPlatform(), dto.getPlatformOrderId());
        return result ? R.success("接单成功") : R.error("接单失败");
    }

    @PostMapping("/sync/menu")
    @Operation(summary = "同步菜品", description = "同步菜单到外卖平台")
    public R<String> syncMenu(@Valid @RequestBody SyncMenuDTO dto) {
        boolean result = deliveryService.syncMenu(dto.getPlatform(), dto.getDishes());
        return result ? R.success("菜单同步成功") : R.error("菜单同步失败");
    }

    @PostMapping("/sync/stock")
    @Operation(summary = "同步库存")
    public R<String> syncStock(@Valid @RequestBody SyncStockDTO dto) {
        boolean result = deliveryService.syncStock(dto.getPlatform(), dto.getStock());
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    @PostMapping("/callback/{platform}")
    @Operation(summary = "平台回调")
    public R<String> callback(@PathVariable String platform, @RequestBody Map<String, String> params) {
        String result = deliveryService.handleCallback(platform, params);
        return R.success(result);
    }
}
