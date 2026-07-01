package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiningTable implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long areaId;
    @TableField(exist = false)
    private String areaName;
    private String name;
    private Integer seatCount;
    private String status;
    private BigDecimal minAmount;
    private String qrCodeUrl;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
