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
 * 订单打印控制器（浏览器本地打印）
 *
 * <p>渲染订单小票纯文本并落一条打印记录（print_task），文本返回前端，
 * 由浏览器调用本地系统打印机打印。门店无需安装打印代理。</p>
 *
 * @author AI
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/printer")
@Tag(name = "订单打印（浏览器调用本地打印机）")
@RequireEmployee
public class PrinterController {

    @Autowired
    private PrinterService printerService;

    /**
     * 渲染订单小票文本并保存打印记录（收银小票 / 厨房单 / 配送单）。
     *
     * @param orderId 订单ID
     * @param type    打印类型：BILL-小票（默认）、KITCHEN-厨房单、DELIVERY-配送单
     * @return 小票纯文本
     */
    @PostMapping("/print/{orderId}")
    @Operation(summary = "渲染并保存打印记录", description = "返回小票文本，前端通过浏览器调用本地打印机打印")
    public R<String> print(@PathVariable("orderId") @Parameter(description = "订单ID") Long orderId,
                           @RequestParam(defaultValue = "BILL") @Parameter(description = "打印类型：BILL/KITCHEN/DELIVERY")
                                   String type) {
        log.info("浏览器打印: orderId={}, type={}", orderId, type);
        return R.success(printerService.renderAndRecord(orderId, type));
    }
}
