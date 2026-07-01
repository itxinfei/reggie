package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MaterialCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String name;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
