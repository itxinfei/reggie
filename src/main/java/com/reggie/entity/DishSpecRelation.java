package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品与规格组关联实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("dish_spec_relation")
@Schema(description = "菜品规格关联")
public class DishSpecRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "菜品ID")
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "规格组ID")
    @NotNull(message = "规格组ID不能为空")
    private Long groupId;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private Long createUser;
}
