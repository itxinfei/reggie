package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/printer/log")
@Tag(name = "打印机日志")
public class PrinterLogController {

    @Autowired
    private PrinterLogService printerLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<PrinterLog>> page(int page, int pageSize, Long orderId) {
        Page<PrinterLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PrinterLog::getOrderId, orderId);
        qw.orderByDesc(PrinterLog::getCreatedTime);
        printerLogService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}
