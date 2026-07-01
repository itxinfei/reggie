package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long userId;
    private Long levelId;
    private String name;
    private String phone;
    private Long points;
    private BigDecimal balance;
    private BigDecimal totalConsumption;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
