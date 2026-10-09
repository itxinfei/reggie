package com.reggie.module.sys.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工-角色关联实体
 * <p>多对多关联：一个员工可拥有多个角色，一个角色可分配给多个员工。
 * 补全 RBAC 闭环（用户→角色→权限），与 {@link RolePermission}（角色→权限）配合使用。
 * <p>注意：类名刻意取 EmployeeRoleRelation，避免与 {@code com.reggie.enums.EmployeeRole}
 * （ADMIN/STAFF 单值枚举）命名冲突。
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@Data
@TableName("employee_role")
@Schema(description = "员工角色关联实体")
public class EmployeeRoleRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工ID", example = "1", required = true)
    private Long employeeId;

    @Schema(description = "角色ID", example = "1", required = true)
    private Long roleId;

    @Schema(description = "租户ID（MP 自动注入）", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
