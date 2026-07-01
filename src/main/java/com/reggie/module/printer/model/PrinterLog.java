package com.reggie.module.printer.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PrinterLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String printType;
    private Long printerId;
    private String content;
    private Integer status;
    private String errorMsg;
    private LocalDateTime createdTime;
}
