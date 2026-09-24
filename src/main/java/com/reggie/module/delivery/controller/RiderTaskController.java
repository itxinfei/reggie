package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireRider;
import com.reggie.module.delivery.dto.RiderTaskVO;
import com.reggie.module.delivery.service.RiderTaskQueryService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 骑手任务接口：我的任务、抢单大厅、任务详情及接单/取餐/送达动作。
 * <p>全部接口仅限骑手账号访问。</p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@RestController
@RequestMapping("/api/rider/tasks")
@RequireRider
@Tag(name = "骑手任务")
public class RiderTaskController {

    @Autowired
    private RiderTaskQueryService riderTaskQueryService;

    @Autowired
    private OrderStatusFlowService orderStatusFlowService;

    @GetMapping("/mine")
    @Operation(summary = "我的任务")
    public R<List<RiderTaskVO>> mine(@RequestParam(value = "scope", defaultValue = "delivering") String scope) {
        Long riderId = BaseContext.getCurrentId();
        return R.success(riderTaskQueryService.listMine(riderId, scope));
    }

    @GetMapping("/hall")
    @Operation(summary = "抢单大厅")
    public R<List<RiderTaskVO>> hall() {
        return R.success(riderTaskQueryService.listHall());
    }

    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public R<RiderTaskVO> detail(@PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        return R.success(riderTaskQueryService.getDetail(id, riderId));
    }

    @PostMapping("/{id}/grab")
    @Operation(summary = "抢单")
    public R<String> grab(@PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        orderStatusFlowService.grabOrder(id, riderId);
        return R.success("抢单成功");
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "确认接单（店长派单）")
    public R<String> accept(@PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        orderStatusFlowService.acceptRiderTask(id, riderId);
        return R.success("已接单");
    }

    @PostMapping("/{id}/pickup")
    @Operation(summary = "确认取餐")
    public R<String> pickup(@PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        orderStatusFlowService.pickupRiderTask(id, riderId);
        return R.success("已取餐");
    }

    @PostMapping("/{id}/deliver")
    @Operation(summary = "确认送达")
    public R<String> deliver(@PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        orderStatusFlowService.deliverRiderOrder(id, riderId);
        return R.success("已送达");
    }
}
