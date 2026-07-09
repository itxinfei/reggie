package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 盘点明细项DTO
 */
@Data
public class StockCheckItemDTO {

    @Schema(description = "食材ID", required = true, example = "1")
    @NotNull(message = "食材ID不能为空")
    private Long materialId;

    @Schema(description = "系统库存", required = true, example = "100.5")
    @NotNull(message = "系统库存不能为空")
    private BigDecimal systemStock;

    @Schema(description = "实际库存", required = true, example = "98.0")
    @NotNull(message = "实际库存不能为空")
    private BigDecimal actualStock;

    @Schema(description = "备注", example = "部分食材损耗")
    private String remark;
}
