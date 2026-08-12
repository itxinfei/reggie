package com.reggie.module.dish.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品规格组实体
 * 例如：份量、辣度、温度等
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("dish_spec_group")
@Schema(description = "菜品规格组")
public class DishSpecGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "规格组名称")
    @NotBlank(message = "规格组名称不能为空")
    private String name;

    @Schema(description = "规格组类型：1-单选，2-多选")
    private Integer type;

    @Schema(description = "是否必选：0-否，1-是")
    private Integer required;

    @Schema(description = "最大可选数量（多选时使用）")
    private Integer maxSelect;

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

