package com.reggie.module.dish.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品规格选项实体
 * 例如：大份、中份、小份、微辣、中辣等
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("dish_spec_option")
@Schema(description = "菜品规格选项")
public class DishSpecOption implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "规格组ID")
    @NotNull(message = "规格组ID不能为空")
    private Long groupId;

    @Schema(description = "选项名称")
    @NotBlank(message = "选项名称不能为空")
    private String name;

    @Schema(description = "价格调整（正数加价，负数减价）")
    private BigDecimal priceAdjust;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "更新人")
    private Long updateUser;
}

