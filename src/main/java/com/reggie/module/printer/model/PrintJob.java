package com.reggie.module.printer.model;

import lombok.Data;
import java.util.List;

@Data
public class PrintJob {
    private Long orderId;
    private String printType;
    private List<PrintLine> lines;
}
