package com.reggie.module.kds.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.kds.service.KitchenTicketService;
import com.reggie.module.kds.vo.KitchenBoardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后厨出餐大屏（KDS）：工单拉取、制作状态流转、看板聚合。全部为后厨员工操作，类级鉴权。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Slf4j
@RestController
@RequestMapping("/api/kds")
@RequireEmployee
@Tag(name = "后厨出餐大屏KDS", description = "工单队列/制作/叫号/出餐")
public class KdsController {

    @Autowired
    private KitchenTicketService kitchenTicketService;

    /**
     * 出餐大屏看板（默认自动拉取新订单）。
     *
     * @param autoPull 是否先拉取新订单，默认 true
     * @return 看板数据
     */
    @GetMapping("/board")
    @Operation(summary = "出餐大屏看板")
    public R<KitchenBoardVO> board(@RequestParam(defaultValue = "true") boolean autoPull) {
        return R.success(kitchenTicketService.getBoard(autoPull));
    }

    /**
     * 手动拉取已下单订单生成工单。
     *
     * @return 新生成工单数
     */
    @PostMapping("/pull")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "拉取新订单生成工单")
    public R<Integer> pull() {
        return R.success(kitchenTicketService.pullPendingOrders());
    }

    /**
     * 开始制作。
     */
    @PutMapping("/start/{id}")
    @Operation(summary = "开始制作")
    public R<KitchenTicket> start(@PathVariable Long id) {
        return R.success(kitchenTicketService.startCook(id));
    }

    /**
     * 制作完成并叫号。
     */
    @PutMapping("/ready/{id}")
    @Operation(summary = "制作完成/叫号")
    public R<KitchenTicket> ready(@PathVariable Long id) {
        return R.success(kitchenTicketService.markReady(id));
    }

    /**
     * 确认出餐/取餐完成。
     */
    @PutMapping("/finish/{id}")
    @Operation(summary = "出餐完成")
    public R<KitchenTicket> finish(@PathVariable Long id) {
        return R.success(kitchenTicketService.finish(id));
    }

    /**
     * 取消工单。
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消工单")
    public R<KitchenTicket> cancel(@PathVariable Long id) {
        return R.success(kitchenTicketService.cancel(id));
    }

    /**
     * 切换加急。
     */
    @PutMapping("/urgent/{id}")
    @Operation(summary = "切换加急标记")
    public R<KitchenTicket> urgent(@PathVariable Long id) {
        return R.success(kitchenTicketService.toggleUrgent(id));
    }

    /**
     * 工单历史分页。
     */
    @GetMapping("/page")
    @Operation(summary = "工单历史分页")
    public R<Page<KitchenTicket>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return R.success(kitchenTicketService.pageTickets(page, pageSize, status));
    }
}
