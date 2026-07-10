package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品口味
 */
@Data
@Schema(description = "菜品口味实体")
public class DishFlavor implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "口味ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "口味名称", example = "辣度", required = true)
    @NotBlank(message = "口味名称不能为空")
    @Size(max = 64, message = "口味名称不能超过64个字符")
    private String name;

    @Schema(description = "口味值", example = "微辣", required = true)
    @NotBlank(message = "口味值不能为空")
    @Size(max = 64, message = "口味值不能超过64个字符")
    private String value;

    @Schema(description = "菜品ID", example = "1", required = true)
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

}
