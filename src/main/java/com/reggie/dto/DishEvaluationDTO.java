package com.reggie.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * <p>
 * 菜品评价数据传输对象
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "菜品评价数据传输对象")
public class DishEvaluationDTO {

    @Schema(description = "评价ID", example = "1")
    private Long id;

    /**
     * 菜品ID（联表查询用，非数据库字段）
     */
    @TableField(exist = false)
    private Long dishId;

    /**
     * 评分
     */
    private Integer starRating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 评价图片JSON数组
     */
    private String images;

    /**
     * 审核状态
     */
    private Integer status;

    /**
     * 商家回复内容
     */
    private String replyContent;

    /**
     * 商家回复时间
     */
    private LocalDateTime replyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 菜品名称（联表查询用，非数据库字段）
     */
    @TableField(exist = false)
    private String dishName;
}
