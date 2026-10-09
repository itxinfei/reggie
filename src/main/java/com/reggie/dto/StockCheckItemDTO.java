package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 盘点明细项DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class StockCheckItemDTO {

    @Schema(description = "食材ID", required = true, example = "1")
    @NotNull(message = "食材ID不能为空")
    private Long materialId;

    @Schema(description = "系统库存（可选，完成盘点时由后端自动读取）", example = "100.5")
    private BigDecimal systemStock;

    @Schema(description = "实际库存（录入实盘和完成盘点时必填）", example = "98.0")
    private BigDecimal actualStock;

    @Schema(description = "备注", example = "部分食材损耗")
    private String remark;
}
