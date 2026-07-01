package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Material implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_NORMAL = 1;
    public static final int STATUS_DISABLED = 0;

    private Long id;
    private Long tenantId;
    private Long categoryId;
    private String name;
    private String unit;
    private BigDecimal stockQty;
    private BigDecimal minStock;
    private BigDecimal unitPrice;
    private Long supplierId;
    private String barcode;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
