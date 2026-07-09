package com.reggie.module.store.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店员工权限关联
 * 记录员工在各门店的角色和权限分配
 */
@Data
@TableName("store_employee_permission")
public class StoreEmployeePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色类型 - 店长 */
    public static final int ROLE_MANAGER = 1;
    /** 角色类型 - 厨师 */
    public static final int ROLE_CHEF = 2;
    /** 角色类型 - 服务员 */
    public static final int ROLE_WAITER = 3;
    /** 角色类型 - 收银员 */
    public static final int ROLE_CASHIER = 4;
    /** 角色类型 - 配送员 */
    public static final int ROLE_DELIVERY = 5;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 员工ID */
    private Long employeeId;

    /** 门店ID */
    private Long tenantId;

    /** 角色类型 */
    private Integer roleType;

    /** 权限列表 JSON数组 */
    private String permissions;

    /** 是否生效 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
