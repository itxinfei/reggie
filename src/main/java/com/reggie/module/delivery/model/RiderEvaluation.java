package com.reggie.module.delivery.model;

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
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 骑手评价实体类（顾客对骑手配送服务的评分）。
 * 每个订单对骑手仅一次有效评价（UNIQUE(order_id, rider_id, is_deleted)），
 * 提交即自动通过并即时计入骑手评分（rider.rating / rider.rating_count）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_evaluation")
@Schema(description = "骑手评价实体")
public class RiderEvaluation implements Serializable {

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

    @Schema(description = "骑手ID", example = "1")
    private Long riderId;

    @Schema(description = "骑手姓名", example = "张骑手")
    private String riderName;

    @Schema(description = "评分（1-5分）", example = "5")
    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能高于5分")
    private Integer starRating;

    @Schema(description = "评价内容", example = "准时送达，态度很好")
    private String content;

    @Schema(description = "评价标签JSON数组，如[\"准时送达\",\"态度友好\"]")
    private String tags;

    @Schema(description = "是否匿名：0=实名，1=匿名（匿名时不展示真实用户名）", example = "0")
    private Integer anonymous;

    @Schema(description = "审核状态：0=待审核，1=通过，2=拒绝（提交即自动通过）", example = "1")
    private Integer status;

    @Schema(description = "商家回复内容")
    private String replyContent;

    @Schema(description = "商家回复时间")
    private LocalDateTime replyTime;

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
