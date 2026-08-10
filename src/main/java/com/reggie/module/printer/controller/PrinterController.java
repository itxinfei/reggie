package com.reggie.module.printer.controller;

import com.reggie.common.R;
import com.reggie.module.printer.adapter.PrinterAdapterFactory;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.module.printer.service.PrinterConfigService;
import com.reggie.module.printer.service.PrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/**
 * 打印机打印控制器
 * 提供订单打印、打印机测试、状态查询等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/printer")
@Tag(name = "打印机打印")
public class PrinterController {

    @Autowired
    private PrinterService printerService;

    @Autowired
    private PrinterAdapterFactory adapterFactory;

    @Autowired
    private PrinterConfigService printerConfigService;

    /**
     * 根据订单ID打印订单小票
     * @param orderId 订单ID
     * @param type 打印类型：BILL-小票（默认）、KITCHEN-厨房单
     * @return 操作结果
     */
    @PostMapping("/print/{orderId}")
    @Operation(summary = "打印订单", description = "根据订单ID打印订单小票，支持多种打印类型（BILL-小票、KITCHEN-厨房单）")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    @Parameter(name = "type", description = "打印类型：BILL-小票（默认）、KITCHEN-厨房单", required = false)
    public R<String> print(@PathVariable Long orderId, @RequestParam(defaultValue = "BILL") String type) {
        log.info("打印订单: orderId={}, type={}", orderId, type);
        printerService.printOrder(orderId, type);
        return R.success("打印任务已发送");
    }

    /**
     * 测试打印机连接
     * @param id 打印机ID
     * @return 连接测试结果
     */
    @PostMapping("/test/{id}")
    @Operation(summary = "测试打印机连接", description = "测试指定打印机是否连接正常")
    @Parameter(name = "id", description = "打印机ID", required = true)
    public R<String> test(@PathVariable Long id) {
        log.info("测试打印机连接: id={}", id);
        boolean ok = printerService.testPrinter(id);
        return ok ? R.success("打印机连接正常") : R.error("打印机连接失败");
    }

    /**
     * 查询打印机状态
     * @param id 打印机ID
     * @return 打印机状态
     */
    @GetMapping("/status/{id}")
    @Operation(summary = "查询打印机状态", description = "查询指定打印机的当前状态（在线/离线/缺纸等）")
    @Parameter(name = "id", description = "打印机ID", required = true)
    public R<PrinterStatus> status(@PathVariable Long id) {
        log.info("查询打印机状态: id={}", id);
        PrinterStatus status = printerService.getPrinterStatus(id);
        return R.success(status);
    }

    /**
     * 根据ID查询打印机配置
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询打印机", description = "根据ID查询打印机配置")
    @Parameter(description = "I d")
    public R<PrinterConfig> getById(@PathVariable Long id) {
        PrinterConfig config = printerConfigService.getById(id);
        if (config != null) {
            return R.success(config);
        }
        return R.error("没有查询到对应打印机");
    }

    /**
     * 获取系统已安装打印机列表
     * @return 系统打印机列表
     */
    @GetMapping("/system/list")
    @Operation(summary = "获取系统已安装打印机列表", description = "获取服务器系统已安装的打印机列表，用于系统打印机绑定")
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

