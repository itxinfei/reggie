package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 完成盘点请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CompleteStockCheckDTO {

    @Schema(description = "盘点明细项列表", required = true)
    @NotNull(message = "盘点明细不能为空")
    private List<StockCheckItemDTO> items;
}
