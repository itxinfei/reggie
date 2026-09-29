package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequireRider;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderExceptionOrder;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.delivery.service.RiderExceptionService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 骑手异常工单控制器：骑手上报/查询、改派候选骑手、管理端分页处理（解决/关闭/改派）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/rider-exception")
@Tag(name = "骑手异常工单", description = "配送异常上报、转单改派与后台处理")
public class RiderExceptionController {

    @Autowired
    private RiderExceptionService riderExceptionService;

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    @Autowired
    private OrderStatusFlowService orderStatusFlowService;

    /**
     * 骑手上报配送异常。
     * @param body 含 orderId、exceptionType、description
     * @return 工单
     */
    @PostMapping
    @RequireRider
    @Operation(summary = "骑手上报异常", description = "对当前在途订单上报配送异常，进入后台待处理队列")
    public R<RiderExceptionOrder> submit(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("orderId") == null) {
            return R.error("订单ID不能为空");
        }
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Integer type = body.get("exceptionType") == null
                ? null : Integer.valueOf(String.valueOf(body.get("exceptionType")));
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        return R.success(riderExceptionService.submit(riderId, tenantId, orderId, type, description));
    }

    /**
     * 骑手查询自己的异常工单。
     */
    @GetMapping("/mine")
    @RequireRider
    @Operation(summary = "我的异常工单", description = "分页查询当前登录骑手上报的异常工单")
    public R<Page<RiderExceptionOrder>> mine(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(riderExceptionService.pageMine(riderId, tenantId, page, PageUtils.cap(size)));
    }

    /**
     * 骑手转单候选：当前租户在线（非离线）且非本人的骑手。
     */
    @GetMapping("/available-riders")
    @RequireRider
    @Operation(summary = "可转单骑手", description = "返回当前门店在线骑手（排除本人），供骑手转单选择")
    public R<List<Map<String, Object>>> availableRiders() {
        Long selfId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Rider> riders = deliveryTrackingService.getRiderList(null, tenantId);
        List<Map<String, Object>> result = new ArrayList<>();
        if (riders != null) {
            for (Rider r : riders) {
                if (r == null || Objects.equals(r.getId(), selfId)
                        || Objects.equals(r.getStatus(), Rider.STATUS_OFFLINE)) {
                    continue;
                }
                Map<String, Object> m = new HashMap<>(4);
                m.put("id", r.getId());
                m.put("name", r.getName());
                m.put("currentOrderCount", r.getCurrentOrderCount());
                result.add(m);
            }
        }
        return R.success(result);
    }

    /**
     * 管理端分页查询异常工单。
     */
    @GetMapping("/admin/page")
    @RequireEmployee
    @Operation(summary = "异常工单分页", description = "支持按类型、状态、订单号筛选，需员工权限")
    public R<Page<RiderExceptionOrder>> adminPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "异常类型：1联系不上顾客 2商品破损 3地址有误 4顾客拒收 5申请转单 6其他")
            @RequestParam(required = false) Integer exceptionType,
            @Parameter(description = "状态：0待处理 1已处理 2已转单 3已关闭")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "订单号（模糊）") @RequestParam(required = false) String orderNumber) {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(riderExceptionService.adminPage(
                tenantId, exceptionType, status, orderNumber, page, PageUtils.cap(size)));
    }

    /**
     * 管理端各状态工单计数（待办徽标）。
     */
    @GetMapping("/admin/count")
    @RequireEmployee
    @Operation(summary = "异常工单状态计数", description = "返回各状态工单数量，供待办徽标展示")
    public R<Map<Integer, Long>> adminCount() {
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(riderExceptionService.countByStatus(tenantId));
    }

    /**
     * 管理端处理工单：解决 / 关闭 / 改派。
     */
    @PutMapping("/{id}/handle")
    @RequireEmployee
    @Operation(summary = "处理异常工单", description = "action=RESOLVE 解决 / CLOSE 关闭 / REASSIGN 改派（需 newRiderId）")
    public R<RiderExceptionOrder> handle(
            @Parameter(description = "工单ID", required = true) @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        if (body == null) {
            return R.error("处理参数不能为空");
        }
        Long operatorId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
        String handleNote = body.get("handleNote") == null ? null : String.valueOf(body.get("handleNote"));
        Long newRiderId = body.get("newRiderId") == null
                ? null : Long.valueOf(String.valueOf(body.get("newRiderId")));
        return R.success(riderExceptionService.handle(id, tenantId, operatorId, action, handleNote, newRiderId));
    }

    /**
     * 管理端直接改派订单（无需先有异常工单）。
     */
    @PostMapping("/admin/reassign")
    @RequireEmployee
    @Operation(summary = "订单改派", description = "把在途订单改派给指定骑手，同步调整双方在途单量")
    public R<String> reassign(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("orderId") == null || body.get("newRiderId") == null) {
            return R.error("订单ID与目标骑手不能为空");
        }
        Long orderId = Long.valueOf(String.valueOf(body.get("orderId")));
        Long newRiderId = Long.valueOf(String.valueOf(body.get("newRiderId")));
        orderStatusFlowService.reassignRiderOrder(orderId, null, newRiderId);
        return R.success("改派成功");
    }
}
