package com.reggie.module.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体
 * 定义系统中可用的角色（店长、厨师、服务员、收银员、配送员等）
 */
@Data
@TableName("role")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 超级管理员角色标识 */
    public static final String ROLE_KEY_ADMIN = "admin";
    /** 店长角色标识 */
    public static final String ROLE_KEY_MANAGER = "manager";
    /** 厨师角色标识 */
    public static final String ROLE_KEY_CHEF = "chef";
    /** 服务员角色标识 */
    public static final String ROLE_KEY_WAITER = "waiter";
    /** 收银员角色标识 */
    public static final String ROLE_KEY_CASHIER = "cashier";
    /** 配送员角色标识 */
    public static final String ROLE_KEY_DELIVERY = "delivery";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID，NULL表示全局角色（所有租户共享） */
    private Long tenantId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识（英文，如chef/waiter/cashier） */
    private String roleKey;

    /** 角色描述 */
    private String description;

    /** 排序，数值越大越靠前 */
    private Integer sort;

    /** 状态 0:禁用 1:启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableLogic
    private Integer isDeleted;
}
