package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 创建采购单请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CreatePurchaseOrderDTO {

    @Schema(description = "供应商ID", required = true, example = "1")
    @NotNull(message = "供应商ID不能为空")
    private Long supplierId;

    @Schema(description = "操作人", required = true, example = "张三")
    @NotNull(message = "操作人不能为空")
    private String operator;

    @Schema(description = "备注", example = "紧急采购")
    private String remark;
}
