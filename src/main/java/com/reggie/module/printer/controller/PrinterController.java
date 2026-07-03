package com.reggie.module.printer.controller;

import com.reggie.common.R;
import com.reggie.module.printer.adapter.PrinterAdapterFactory;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.module.printer.service.PrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.print.PrintService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/printer")
@Tag(name = "打印机打印")
public class PrinterController {

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PrinterAdapterFactory adapterFactory;

    @PostMapping("/print/{orderId}")
    @Operation(summary = "打印订单")
    public R<String> print(@PathVariable Long orderId, @RequestParam(defaultValue = "BILL") String type) {
        log.info("打印订单: orderId={}, type={}", orderId, type);
        printerService.printOrder(orderId, type);
        return R.success("打印任务已发送");
    }

    @PostMapping("/test/{id}")
    @Operation(summary = "测试打印机连接")
    public R<String> test(@PathVariable Long id) {
        log.info("测试打印机连接: id={}", id);
        boolean ok = printerService.testPrinter(id);
        return ok ? R.success("打印机连接正常") : R.error("打印机连接失败");
    }

    @GetMapping("/status/{id}")
    @Operation(summary = "查询打印机状态")
    public R<PrinterStatus> status(@PathVariable Long id) {
        log.info("查询打印机状态: id={}", id);
        PrinterStatus status = printerService.getPrinterStatus(id);
        return R.success(status);
    }

    @GetMapping("/system/list")
    @Operation(summary = "获取系统已安装打印机列表")
    public R<List<Map<String, String>>> listSystemPrinters() {
        log.info("获取系统打印机列表");
        List<PrintService> services = adapterFactory.getWindowsAdapter().listSystemPrinters();
        List<Map<String, String>> result = new ArrayList<>();

        for (PrintService service : services) {
            Map<String, String> printerInfo = new HashMap<>();
            printerInfo.put("name", service.getName());
            printerInfo.put("type", service.getClass().getSimpleName());
            result.add(printerInfo);
        }

        log.info("系统打印机数量: {}", result.size());
        return R.success(result);
    }
}