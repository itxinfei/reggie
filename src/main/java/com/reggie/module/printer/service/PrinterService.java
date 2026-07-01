package com.reggie.module.printer.service;

import com.reggie.module.printer.model.PrinterStatus;

public interface PrinterService {
    void printOrder(Long orderId, String printType);
    boolean testPrinter(Long printerId);
    PrinterStatus getPrinterStatus(Long printerId);
}
