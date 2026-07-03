package com.reggie.module.printer.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrinterAdapterFactory {

    @Autowired
    private GprinterAdapter gprinterAdapter;

    @Autowired
    private XprinterAdapter xprinterAdapter;

    @Autowired
    private WindowsSystemPrinterAdapter windowsSystemPrinterAdapter;

    public PrinterAdapter getAdapter(String brand) {
        if (brand == null) {
            return windowsSystemPrinterAdapter;
        }
        switch (brand.toUpperCase()) {
            case "GPRINTER":
                return gprinterAdapter;
            case "XPRINTER":
                return xprinterAdapter;
            case "WINDOWS":
                return windowsSystemPrinterAdapter;
            case "SYSTEM":
                return windowsSystemPrinterAdapter;
            default:
                return windowsSystemPrinterAdapter;
        }
    }

    public WindowsSystemPrinterAdapter getWindowsAdapter() {
        return windowsSystemPrinterAdapter;
    }
}