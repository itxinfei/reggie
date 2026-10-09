package com.reggie.module.subsidy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 餐补发放请求 DTO
 * <p>二选一：按单个用户发放（userId）或按部门批量发放（departmentId，
 * 发放对象为该租户挂此部门的全部正常顾客用户）。两者都传时优先按部门。</p>
 */
@Data
@Schema(description = "餐补发放请求")
public class SubsidyGrantDTO {

    @Schema(description = "目标用户ID（单个发放，与 departmentId 二选一）", example = "1")
    private Long userId;

    @Schema(description = "目标部门ID（批量发放该部门全部用户，优先于 userId）", example = "1")
    private Long departmentId;

    @Schema(description = "发放金额（元）", required = true, example = "100.00")
    @NotNull(message = "发放金额不能为空")
    @DecimalMin(value = "0.01", message = "发放金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "备注", example = "10月餐补")
    private String remark;
}
