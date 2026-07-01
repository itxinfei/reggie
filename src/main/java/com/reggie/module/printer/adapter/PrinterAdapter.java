package com.reggie.module.printer.adapter;

import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;

public interface PrinterAdapter {
    boolean print(PrintJob job, PrinterConfig config);
    PrinterStatus queryStatus(PrinterConfig config);
    boolean testConnection(PrinterConfig config);
}
