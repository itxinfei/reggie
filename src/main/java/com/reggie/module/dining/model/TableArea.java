package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dining_area")
public class TableArea implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String name;
    private Integer sort;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
