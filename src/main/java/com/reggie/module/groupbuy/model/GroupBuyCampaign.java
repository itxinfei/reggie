package com.reggie.module.groupbuy.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拼团活动
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("group_buy_campaign")
@Schema(description = "拼团活动")
public class GroupBuyCampaign implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "拼团活动ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "活动名称", example = "土豆拼团优惠")
    private String name;

    @Schema(description = "活动描述")
    private String description;

    @Schema(description = "拼团组ID（同一活动可多组并发）", example = "1")
    private Long groupId;

    @Schema(description = "状态：OPEN=进行中，CLOSED=已关闭，ENDED=已结束", example = "OPEN")
    private String status;

    @Schema(description = "开始时间", example = "2026-09-01 00:00:00")
    private LocalDateTime startTime;

    @Schema(description = "结束时间", example = "2026-09-07 23:59:59")
    private LocalDateTime endTime;

    @Schema(description = "最少成团人数", example = "2")
    private Integer minMembers;

    @Schema(description = "最多成团人数", example = "10")
    private Integer maxMembers;

    @Schema(description = "原价（元）", example = "50.00")
    private java.math.BigDecimal originalPrice;

    @Schema(description = "拼团价（元）", example = "39.90")
    private java.math.BigDecimal groupPrice;

    @Schema(description = "菜品ID", example = "1")
    private Long dishId;

    @Schema(description = "菜品名称", example = "酸辣土豆丝")
    private String dishName;

    @Schema(description = "活动图片URL")
    private String image;

    @Schema(description = "创建时间", example = "2026-09-01 10:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-09-01 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
