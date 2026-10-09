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
 * 拼团参与记录
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("group_buy_participation")
@Schema(description = "拼团参与记录")
public class GroupBuyParticipation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "参与记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "拼团活动ID", example = "1")
    private Long groupBuyId;

    @Schema(description = "订单ID", example = "1")
    private Long orderId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "状态：JOINED=已参团，PAID=已支付，CANCELLED=已取消", example = "JOINED")
    private String status;

    @Schema(description = "参团时间", example = "2026-09-01 10:30:00")
    private LocalDateTime joinTime;

    @Schema(description = "支付时间", example = "2026-09-01 10:35:00")
    private LocalDateTime payTime;

    @Schema(description = "创建时间", example = "2026-09-01 10:30:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
