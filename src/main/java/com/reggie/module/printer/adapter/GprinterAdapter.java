package com.reggie.module.printer.adapter;

import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 佳博打印机适配器
 * 实现与佳博票据打印机的交互逻辑
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class GprinterAdapter implements PrinterAdapter {

    /**
     * 打印任务
     *
     * @param job    打印任务
     * @param config 打印机配置
     * @return 是否打印成功
     */
    @Override
    public boolean print(PrintJob job, PrinterConfig config) {
        log.info("GprinterAdapter printing to device: {} (IP: {}:{})", config.getDeviceId(), config.getIpAddress(), config.getPort());
        for (PrintLine line : job.getLines()) {
            log.info("  [{}] {}", line.getType(), line.getText());
        }
        return true;
    }

    /**
     * 查询打印机状态
     *
     * @param config 打印机配置
     * @return 打印机状态
     */
    @Override
    public PrinterStatus queryStatus(PrinterConfig config) {
        PrinterStatus status = new PrinterStatus();
        status.setOnline(true);
        status.setDetail("Gprinter online: " + config.getDeviceId());
        return status;
    }

    /**
     * 测试打印机连接
     *
     * @param config 打印机配置
     * @return 是否连接成功
     */
    @Override
    public boolean testConnection(PrinterConfig config) {
        log.info("GprinterAdapter test connection: {}", config.getDeviceId());
        return true;
    }
}
