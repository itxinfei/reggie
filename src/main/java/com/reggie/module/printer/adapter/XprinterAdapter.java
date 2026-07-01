package com.reggie.module.printer.adapter;

import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XprinterAdapter implements PrinterAdapter {

    @Override
    public boolean print(PrintJob job, PrinterConfig config) {
        log.info("XprinterAdapter printing to device: {} (IP: {}:{})", config.getDeviceId(), config.getIpAddress(), config.getPort());
        for (PrintLine line : job.getLines()) {
            log.info("  [{}] {}", line.getType(), line.getText());
        }
        return true;
    }

    @Override
    public PrinterStatus queryStatus(PrinterConfig config) {
        PrinterStatus status = new PrinterStatus();
        status.setOnline(true);
        status.setDetail("Xprinter online: " + config.getDeviceId());
        return status;
    }

    @Override
    public boolean testConnection(PrinterConfig config) {
        log.info("XprinterAdapter test connection: {}", config.getDeviceId());
        return true;
    }
}
