package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新增部门 DTO
 * <p>仅包含前端需要填写的业务字段，tenantId / id / isDeleted 等敏感字段
 * 由服务端通过 BaseContext 自动填充，前端无法篡改租户归属。</p>
 */
@Data
@Schema(description = "新增部门请求")
public class DepartmentSaveDTO {

    @Schema(description = "部门名称", example = "技术部")
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称不能超过50个字符")
    private String name;

    @Schema(description = "部门编码", example = "TECH")
    @Size(max = 32, message = "部门编码不能超过32个字符")
    private String code;

    @Schema(description = "负责人姓名", example = "张三")
    @Size(max = 50, message = "负责人姓名不能超过50个字符")
    private String leaderName;

    @Schema(description = "备注", example = "研发中心")
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    @Schema(description = "排序（数值越小越靠前）", example = "1")
    private Integer sort;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;
}
