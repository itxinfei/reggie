package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物料分类实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class MaterialCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
