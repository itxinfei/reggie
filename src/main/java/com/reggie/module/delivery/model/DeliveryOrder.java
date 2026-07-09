package com.reggie.module.delivery.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeliveryOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String platformOrderId;
    private String platform;
    private String dishSummary;
    private BigDecimal amount;
    private String userName;
    private String phone;
    private String address;
    private String status;
    private LocalDateTime orderTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long createdUser;
    private Long updatedUser;
}
