package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String name;
    private String type;
    private BigDecimal conditionAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalCount;
    private Integer remainCount;
    private Integer validDays;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
