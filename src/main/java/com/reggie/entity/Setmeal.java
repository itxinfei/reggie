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
 * 套餐
 */
@Data
@TableName("setmeal")
@Schema(description = "套餐")
public class Setmeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "套餐分类ID", example = "2", required = true)
    @NotNull(message = "套餐分类不能为空")
    private Long categoryId;

    @Schema(description = "套餐名称", example = "超值套餐", required = true)
    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 64, message = "套餐名称不能超过64个字符")
    private String name;

    @Schema(description = "套餐价格", example = "88.00", required = true)
    @NotNull(message = "套餐价格不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "套餐价格必须大于0")
    private BigDecimal price;

    @Schema(description = "套餐状态：0=停用，1=启用", example = "1", required = true)
    @NotNull(message = "套餐状态不能为空")
    private Integer status;

    @Schema(description = "套餐编码", example = "SET001")
    @Size(max = 64, message = "编码不能超过64个字符")
    private String code;

    @Schema(description = "套餐描述", example = "包含多道菜品的超值组合")
    @Size(max = 400, message = "描述信息不能超过400个字符")
    private String description;

    @Schema(description = "套餐图片", example = "https://xxx.com/1.jpg")
    @Size(max = 200, message = "图片路径不能超过200个字符")
    private String image;

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

}
