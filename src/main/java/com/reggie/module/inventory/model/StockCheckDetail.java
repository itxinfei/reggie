package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class StockCheckDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long checkId;
    private Long materialId;
    private BigDecimal bookQty;
    private BigDecimal actualQty;
    private BigDecimal diffQty;
    private String remark;
}
