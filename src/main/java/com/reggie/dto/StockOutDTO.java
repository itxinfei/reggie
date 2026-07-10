package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 出库请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class StockOutDTO {

    @Schema(description = "食材ID", required = true, example = "1")
    @NotNull(message = "食材ID不能为空")
    private Long materialId;

    @Schema(description = "出库数量", required = true, example = "10")
    @NotNull(message = "出库数量不能为空")
    @DecimalMin(value = "0.01", message = "出库数量必须大于0")
    private BigDecimal qty;

    @Schema(description = "业务ID", example = "1")
    private Long bizId;

    @Schema(description = "备注", example = "领用出库")
    private String remark;

    @Schema(description = "操作人", required = true, example = "张三")
    @NotNull(message = "操作人不能为空")
    private String operator;
}
