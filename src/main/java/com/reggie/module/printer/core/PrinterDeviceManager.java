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

@Slf4j
@Component
public class PrinterDeviceManager {

    @Autowired
    private PrinterConfigService printerConfigService;

    @Autowired
    private PrinterAdapterFactory adapterFactory;

    public List<PrinterConfig> findPrinters(String printType) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PrinterConfig> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.like(PrinterConfig::getPrintTypes, printType);
        qw.eq(PrinterConfig::getStatus, 1);
        return printerConfigService.list(qw);
    }

    public boolean dispatch(PrintJob job, PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.print(job, config);
    }

    public PrinterStatus queryStatus(PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.queryStatus(config);
    }

    public boolean testConnection(PrinterConfig config) {
        PrinterAdapter adapter = adapterFactory.getAdapter(config.getBrand());
        return adapter.testConnection(config);
    }
}
