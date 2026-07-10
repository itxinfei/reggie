package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 打印机日志管理控制器
 * 提供打印日志的分页查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/printer/log")
@Tag(name = "打印机日志")
public class PrinterLogController {

    @Autowired
    private PrinterLogService printerLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询打印日志，支持按订单ID、打印类型、状态、时间范围筛选")
    public R<Page<PrinterLog>> page(
            int page,
            int pageSize,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String printType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {

        Page<PrinterLog> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> qw = new LambdaQueryWrapper<>();
        qw.eq(orderId != null, PrinterLog::getOrderId, orderId);
        qw.eq(StringUtils.isNotBlank(printType), PrinterLog::getPrintType, printType);
        qw.eq(status != null, PrinterLog::getStatus, status);
        if (StringUtils.isNotBlank(beginTime)) {
            qw.ge(PrinterLog::getCreatedTime, LocalDate.parse(beginTime).atStartOfDay());
        }
        if (StringUtils.isNotBlank(endTime)) {
            qw.le(PrinterLog::getCreatedTime, LocalDate.parse(endTime).atTime(LocalTime.MAX));
        }
        qw.orderByDesc(PrinterLog::getCreatedTime);
        printerLogService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}

