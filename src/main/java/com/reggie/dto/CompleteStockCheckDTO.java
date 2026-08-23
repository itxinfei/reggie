package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * <p>
 * 完成盘点请求DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "完成盘点请求DTO")
public class CompleteStockCheckDTO {

    @Schema(description = "盘点明细项列表", required = true)
    @NotNull(message = "盘点明细不能为空")
    @Size(max = 200, message = "盘点明细不能超过200项")
    private List<StockCheckItemDTO> items;
}
