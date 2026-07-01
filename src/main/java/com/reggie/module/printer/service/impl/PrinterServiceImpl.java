package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.CustomException;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.module.printer.core.PrinterDeviceManager;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.module.printer.service.PrinterConfigService;
import com.reggie.module.printer.service.PrinterLogService;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PrinterServiceImpl implements PrinterService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private PrinterTemplate printerTemplate;

    @Autowired
    private PrinterDeviceManager printerDeviceManager;

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private PrinterLogService printerLogService;

    @Override
    public void printOrder(Long orderId, String printType) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }

        LambdaQueryWrapper<PrinterConfig> qw = new LambdaQueryWrapper<>();
        qw.eq(PrinterConfig::getPrintType, printType);
        qw.eq(PrinterConfig::getStatus, 1);
        List<PrinterConfig> printers = printerConfigService.list(qw);
        if (printers.isEmpty()) {
            log.warn("No enabled printer found for type: {}", printType);
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
                plog.setErrorMsg("Print failed");
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
