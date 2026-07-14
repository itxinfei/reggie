package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品
 */
@Data
@TableName("dish")
@Schema(description = "菜品")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "菜品名称", example = "鱼香肉丝", required = true)
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称不能超过64个字符")
    private String name;

    @Schema(description = "菜品分类ID", example = "1", required = true)
    @NotNull(message = "菜品分类不能为空")
    private Long categoryId;

    @Schema(description = "菜品价格", example = "38.00", required = true)
    @NotNull(message = "菜品价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "菜品价格必须大于0")
    private BigDecimal price;

    @Schema(description = "商品码", example = "6901234567890")
    @Size(max = 64, message = "商品码不能超过64个字符")
    private String code;

    @Schema(description = "菜品图片", example = "https://xxx.com/1.jpg")
    @Size(max = 200, message = "图片路径不能超过200个字符")
    private String image;

    @Schema(description = "菜品描述", example = "经典川菜，微辣")
    @Size(max = 400, message = "描述信息不能超过400个字符")
    private String description;

    @Schema(description = "菜品状态：0=停售，1=起售", example = "1", required = true)
    @NotNull(message = "菜品状态不能为空")
    private Integer status;

    @Schema(description = "排序（升序）", example = "1")
    private Integer sort;

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
    private Integer isDeleted;

    @Schema(description = "当前库存数量", example = "100")
    private java.math.BigDecimal stockQty;

    @Schema(description = "最低库存预警阈值", example = "10")
    private java.math.BigDecimal minStock;

}
