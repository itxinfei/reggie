package com.reggie.module.sys.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色实体
 * 定义系统中可用的角色（店长、厨师、服务员、收银员、配送员等）
 */
@Data
@TableName("role")
@Schema(description = "角色实体")
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

    @Schema(description = "角色ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID（NULL表示全局角色）", example = "1")
    private Long tenantId;

    @Schema(description = "角色名称", example = "店长", required = true)
    private String roleName;

    @Schema(description = "角色标识", example = "manager", required = true)
    private String roleKey;

    @Schema(description = "角色描述", example = "店铺管理员，拥有所有权限")
    private String description;

    @Schema(description = "排序（数值越大越靠前）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic
    private Integer isDeleted;
}
