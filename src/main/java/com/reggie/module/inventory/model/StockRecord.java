package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long materialId;
    private String type;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private Long bizId;
    private String remark;
    private String operator;
    private LocalDateTime createdTime;
}
