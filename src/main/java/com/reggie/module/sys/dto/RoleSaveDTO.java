package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 新增角色 DTO
 * <p>仅包含前端需要填写的业务字段，tenantId / id / isDeleted 等敏感字段
 * 由服务端通过 BaseContext 自动填充，前端无法篡改租户归属。</p>
 */
@Data
@Schema(description = "新增角色请求")
public class RoleSaveDTO {

    @Schema(description = "角色名称", example = "店长")
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "角色标识", example = "manager")
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    @Schema(description = "角色描述", example = "店铺管理员")
    private String description;

    @Schema(description = "排序（数值越大越靠前）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;
}