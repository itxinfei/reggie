package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 同步库存到外卖平台请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class SyncStockDTO {

    @Schema(description = "外卖平台（MEITUAN-美团、ELEME-饿了么）", required = true, example = "MEITUAN")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "库存数据（Key: 商品ID, Value: 库存数量）", required = true)
    @NotNull(message = "库存数据不能为空")
    private Map<Long, Integer> stock;
}
