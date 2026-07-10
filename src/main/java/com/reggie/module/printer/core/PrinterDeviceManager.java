package com.reggie.module.printer.core;

import com.reggie.module.printer.adapter.PrinterAdapter;
import com.reggie.module.printer.adapter.PrinterAdapterFactory;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import com.reggie.module.printer.service.PrinterConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 打印机设备管理器
 * 统一管理打印机设备的发现、打印、状态查询等操作
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class PrinterDeviceManager {

    /** 打印机配置服务 */
    @Autowired
    private PrinterConfigService printerConfigService;

    /** 打印机适配器工厂 */
    @Autowired
    private PrinterAdapterFactory adapterFactory;

    /**
     * 根据打印类型查找可用的打印机
     *
     * @param printType 打印类型
     * @return 打印机配置列表
     */
    public List<PrinterConfig> findPrinters(String printType) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrinterConfig> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.like(PrinterConfig::getPrintTypes, printType);
        qw.eq(PrinterConfig::getStatus, 1);
        return printerConfigService.list(qw);
    }

    /**
     * 分发打印任务到指定打印机
     *
     * @param job    打印任务
     * @param config 打印机配置
     * @return 是否打印成功
     */
    public boolean dispatch(PrintJob job, PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.print(job, config);
    }

    /**
     * 查询打印机状态
     *
     * @param config 打印机配置
     * @return 打印机状态
     */
    public PrinterStatus queryStatus(PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.queryStatus(config);
    }

    /**
     * 测试打印机连接
     *
     * @param config 打印机配置
     * @return 是否连接成功
     */
    public boolean testConnection(PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.testConnection(config);
    }
}
