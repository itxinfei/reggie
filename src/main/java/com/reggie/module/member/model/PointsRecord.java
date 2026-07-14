package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分记录
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("points_record")
@Schema(description = "积分记录")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "类型：earn=获取，consume=消费抵扣", example = "earn")
    private String type;

    @Schema(description = "积分变动数量（正数为获取，负数为消耗）", example = "50")
    private Integer points;

    @Schema(description = "关联业务类型", example = "order")
    private String bizType;

    @Schema(description = "关联业务ID", example = "1")
    private Long bizId;

    @Schema(description = "备注说明", example = "订单消费赠送积分")
    private String remark;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
