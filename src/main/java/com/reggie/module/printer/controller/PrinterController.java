package com.reggie.module.printer.controller;

import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.printer.service.PrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单打印控制器（门店 PC 本地打印）
 *
 * <p>将订单打印任务入队（print_task），由门店 PC 打印代理领取后调用本地打印机。
 * 已移除旧的服务器直连打印（/test、/status、/system/list 等设备接口）。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@RestController
@RequestMapping("/printer")
@Tag(name = "订单打印（入队门店PC终端）")
@RequireEmployee
public class PrinterController {

    @Autowired
    private PrinterService printerService;

    /**
     * 根据订单ID派发打印任务（收银小票 / 厨房单 / 配送单）。
     *
     * @param orderId 订单ID
     * @param type    打印类型：BILL-小票（默认）、KITCHEN-厨房单、DELIVERY-配送单
     * @return 操作结果
     */
    @PostMapping("/print/{orderId}")
    @Operation(summary = "派发订单打印任务", description = "按订单租户匹配门店PC打印代理终端并入队任务")
    public R<String> print(@PathVariable("orderId") @Parameter(description = "订单ID") Long orderId,
                           @RequestParam(defaultValue = "BILL") @Parameter(description = "打印类型：BILL/KITCHEN/DELIVERY")
                                   String type) {
        log.info("派发订单打印任务: orderId={}, type={}", orderId, type);
        printerService.printOrder(orderId, type);
        return R.success("打印任务已派发");
    }
}
