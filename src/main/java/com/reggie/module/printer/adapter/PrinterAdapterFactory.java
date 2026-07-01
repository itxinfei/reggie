package com.reggie.module.printer.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrinterAdapterFactory {

    @Autowired
    private GprinterAdapter gprinterAdapter;

    @Autowired
    private XprinterAdapter xprinterAdapter;

    public PrinterAdapter getAdapter(String brand) {
        if (brand == null) {
            return gprinterAdapter;
        }
        switch (brand.toUpperCase()) {
            case "GPRINTER":
                return gprinterAdapter;
            case "XPRINTER":
                return xprinterAdapter;
            default:
                return gprinterAdapter;
        }
    }
}
