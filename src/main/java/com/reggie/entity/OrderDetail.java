package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * 订单明细
 */
@Data
@Schema(description = "订单明细")
public class OrderDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "明细ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品名称", example = "鱼香肉丝", required = true)
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "订单ID", example = "1", required = true)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

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

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建时间", example = "2024-01-01 12:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 12:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID", example = "1")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是", example = "0")
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

}
