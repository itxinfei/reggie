package com.reggie.module.sys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 修改部门 DTO
 * <p>在新增字段基础上要求 id；Service 层先校验租户归属再更新业务字段。</p>
 */
@Data
@Schema(description = "修改部门请求")
public class DepartmentUpdateDTO {

    @Schema(description = "部门ID", example = "1")
    @NotNull(message = "部门ID不能为空")
    private Long id;

    @Schema(description = "部门名称", example = "技术部")
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
