package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.CustomException;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.printer.core.PrinterDeviceManager;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.printer.service.PrinterConfigService;
import com.reggie.module.printer.service.PrinterLogService;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印机服务实现
 *
 * @author reggie
 * @since 2026-07-09
 *
 * <p>注意：本类<b>禁止</b>类级 @Transactional。
 * 历史实现曾用类级事务，导致下单流程中 printOrder 抛异常（如打印机表缺失、打印失败）时，
 * 内层事务被标记 rollback-only，即使被调用方 try-catch 吞掉，外层订单事务提交仍抛
 * UnexpectedRollbackException——整笔下单被打印故障拖垮，与"打印失败不影响下单"的语义相悖。
 * 本类方法均为查询 + 单条日志插入，无需强事务。</p>
 */
@Slf4j
@Service
public class PrinterServiceImpl implements PrinterService {

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 打印模板 */
    @Autowired
    private PrinterTemplate printerTemplate;

    /** 打印机设备管理器 */
    @Autowired
    private PrinterDeviceManager printerDeviceManager;

    /** 打印机配置服务 */
    @Autowired
    private PrinterConfigService printerConfigService;

    /** 打印日志服务 */
    @Autowired
    private PrinterLogService printerLogService;

    @Override
    public void printOrder(Long orderId, String printType) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }

        // 查询所有启用的打印机，支持 printTypes 包含当前 printType
        LambdaQueryWrapper<PrinterConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(PrinterConfig::getStatus, 1);
        qw.apply("CONCAT(',', print_types, ',') LIKE CONCAT('%,', {0}, ',%')", printType);
        List<PrinterConfig> printers = printerConfigService.list(qw);
        if (printers.isEmpty()) {
            log.warn("未找到已启用的打印机，类型: {}", printType);
            return;
        }

        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        PrintJob job = printerTemplate.build(order, details, printType);

        for (PrinterConfig printer : printers) {
            boolean success = printerDeviceManager.dispatch(job, printer);

            PrinterLog plog = new PrinterLog();
            plog.setOrderId(orderId);
            plog.setPrintType(printType);
            plog.setPrinterId(printer.getId());
            plog.setStatus(success ? 1 : 0);
            plog.setContent(job.getLines().toString());
            plog.setCreatedTime(LocalDateTime.now());
            if (!success) {
                plog.setErrorMsg("打印失败");
            }
            printerLogService.save(plog);
        }
    }

    @Override
    public boolean testPrinter(Long printerId) {
        PrinterConfig config = printerConfigService.getById(printerId);
        if (config == null) {
            throw new CustomException("打印机配置不存在");
        }
        return printerDeviceManager.testConnection(config);
    }

    @Override
    public PrinterStatus getPrinterStatus(Long printerId) {
        PrinterConfig config = printerConfigService.getById(printerId);
        if (config == null) {
            throw new CustomException("打印机配置不存在");
        }
        return printerDeviceManager.queryStatus(config);
    }
}




