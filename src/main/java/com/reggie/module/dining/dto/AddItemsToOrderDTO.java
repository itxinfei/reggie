package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 加菜请求 DTO（为已有堂食订单追加菜品）
 *
 * @author reggie
 * @since 2026-09-18
 */
@Data
@Schema(description = "加菜请求")
public class AddItemsToOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID", required = true, example = "1")
    private Long orderId;

    @NotNull(message = "加菜列表不能为空")
    @Schema(description = "加菜明细列表", required = true)
    @Valid
    private List<OrderItem> items;

    /**
     * 单个加菜项
     */
    @Data
    @Schema(description = "加菜项")
    public static class OrderItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "菜品ID（菜品/套餐二选一）", example = "1")
        private Long dishId;

        @Schema(description = "套餐ID（菜品/套餐二选一）", example = "1")
        private Long setmealId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量必须大于0")
        @Schema(description = "数量", required = true, example = "1")
        private Integer number;

        @Schema(description = "口味（可选）", example = "微辣")
        private String flavor;

        @Schema(description = "备注（可选）", example = "少放辣")
        private String remark;
    }
}
