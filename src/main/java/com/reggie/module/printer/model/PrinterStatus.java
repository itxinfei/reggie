package com.reggie.module.printer.model;

import lombok.Data;

@Data
public class PrinterStatus {
    private boolean online;
    private String detail;
}
