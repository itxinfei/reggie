package com.reggie.module.printer.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PrinterConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long storeId;
    private String name;
    private String type;
    private String brand;
    private String deviceId;
    private String ipAddress;
    private Integer port;
    private String paperSize;
    private String printType;
    private Integer status;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
