package com.reggie.module.inventory.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class Supplier implements Serializable {
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
     * 供应商名称
     */
    private String name;

    /**
     * 联系人
     */
    private String contact;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 地址
     */
    private String address;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
