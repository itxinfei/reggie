package com.reggie.module.dish.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品评价实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("dish_evaluation")
@Schema(description = "菜品评价实体")
public class DishEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID", example = "1")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "评价用户ID", example = "1")
    private Long userId;

    @Schema(description = "评价用户名", example = "张三")
    private String userName;

    @Schema(description = "评价菜品ID", example = "1")
    private Long dishId;

    @Schema(description = "菜品名称", example = "鱼香肉丝")
    private String dishName;

    @Schema(description = "评分（1-5分）", example = "5")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    private Integer starRating;

    @Schema(description = "评价内容", example = "味道很好，分量足")
    private String content;

    @Schema(description = "评价图片JSON数组", example = "[\"https://xxx.com/1.jpg\"]")
    private String images;

    @Schema(description = "商家回复内容", example = "感谢您的好评！")
    private String replyContent;

    @Schema(description = "商家回复时间")
    private LocalDateTime replyTime;

    @Schema(description = "审核状态：0=待审核，1=通过，2=拒绝", example = "1")
    private Integer status;

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

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}

