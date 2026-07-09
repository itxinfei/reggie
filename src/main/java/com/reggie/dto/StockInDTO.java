package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 入库请求DTO
 */
@Data
public class StockInDTO {

    @Schema(description = "食材ID", required = true, example = "1")
    @NotNull(message = "食材ID不能为空")
    private Long materialId;

    @Schema(description = "入库数量", required = true, example = "50")
    @NotNull(message = "入库数量不能为空")
    @DecimalMin(value = "0.01", message = "入库数量必须大于0")
    private BigDecimal qty;

    @Schema(description = "入库单价", example = "15.50")
    private BigDecimal unitPrice;

    @Schema(description = "业务ID（如采购单ID）", example = "1")
    private Long bizId;

    @Schema(description = "备注", example = "采购入库")
    private String remark;

    @Schema(description = "操作人", required = true, example = "张三")
    @NotNull(message = "操作人不能为空")
    private String operator;
}
