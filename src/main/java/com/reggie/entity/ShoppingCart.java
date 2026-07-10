package com.reggie.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车
 */
@Data
@Schema(description = "购物车实体")
public class ShoppingCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车ID", example = "1")
    private Long id;

    @Schema(description = "商品名称", example = "鱼香肉丝", required = true)
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "用户ID", example = "1", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "菜品ID（菜品时必填）", example = "1")
    private Long dishId;

    @Schema(description = "套餐ID（套餐时必填）", example = "1")
    private Long setmealId;

    @Schema(description = "口味", example = "微辣")
    private String dishFlavor;

    @Schema(description = "数量", example = "1", required = true)
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量必须大于0")
    private Integer number;

    @Schema(description = "金额", example = "38.00", required = true)
    @NotNull(message = "商品金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "商品金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "商品图片", example = "https://xxx.com/1.jpg")
    private String image;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
