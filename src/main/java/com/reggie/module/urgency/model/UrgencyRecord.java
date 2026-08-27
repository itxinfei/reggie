package com.reggie.module.urgency.model;

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
 * 催单记录实体
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@TableName("urgency_record")
@Schema(description = "催单记录")
public class UrgencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "订单ID", example = "2001")
    private Long orderId;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "订单号", example = "ORD20260827001")
    private String orderNo;

    @Schema(description = "催单次数", example = "1")
    private Integer times;

    @Schema(description = "状态：SENT=已发送, PROCESSED=已处理, IGNORED=已忽略", example = "SENT")
    private String status;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
