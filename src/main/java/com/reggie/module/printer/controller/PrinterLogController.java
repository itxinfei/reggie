package com.reggie.module.printer.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RestController
@RequestMapping("/printer/log")
@Tag(name = "打印机日志")
@RequireEmployee
public class PrinterLogController {

    @Autowired
    private PrinterLogService printerLogService;

    /**
     * 分页查询打印日志
     * @param page 页码
     * @param pageSize 每页条数
     * @param orderId 订单ID（可选）
     * @param printType 打印类型（可选）
     * @param status 状态（可选）
     * @param beginTime 开始日期（可选）
     * @param endTime 结束日期（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询打印日志", description = "分页查询打印日志，支持按订单ID、打印类型、状态、时间范围筛选")
    public R<Page<PrinterLog>> page(
            @Parameter(description = "页码") int page,
            @Parameter(description = "每页条数") int pageSize,
            @Parameter(description = "订单ID（可选）") @RequestParam(required = false) Long orderId,
            @Parameter(description = "打印类型（可选）") @RequestParam(required = false) String printType,
            @Parameter(description = "状态（可选）") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false) String beginTime,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) String endTime) {

        Page<PrinterLog> pageInfo = PageUtils.of(page, pageSize);
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

