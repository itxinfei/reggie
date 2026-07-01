package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String checkNo;
    private String status;
    private BigDecimal totalDiffAmount;
    private String operator;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
