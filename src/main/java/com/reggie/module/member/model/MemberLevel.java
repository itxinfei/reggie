package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MemberLevel implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String name;
    private Long minPoints;
    private BigDecimal discount;
    private LocalDateTime createdTime;
}
