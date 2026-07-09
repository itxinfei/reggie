package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 添加采购单明细请求DTO
 */
@Data
public class AddPurchaseDetailDTO {

    @Schema(description = "采购单ID", required = true, example = "1")
    @NotNull(message = "采购单ID不能为空")
    private Long orderId;

    @Schema(description = "食材ID", required = true, example = "1")
    @NotNull(message = "食材ID不能为空")
    private Long materialId;

    @Schema(description = "数量", required = true, example = "10")
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.01", message = "数量必须大于0")
    private BigDecimal qty;

    @Schema(description = "单价", required = true, example = "15.50")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.01", message = "单价必须大于0")
    private BigDecimal unitPrice;
}
