package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 更新角色 DTO
 * <p>前端仅提交 id + 业务字段。tenantId 由服务端通过 BaseContext 校验归属，
 * 不允许前端指定。</p>
 */
@Data
@Schema(description = "更新角色请求")
public class RoleUpdateDTO {

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long id;

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