package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("recharge_record")
@Schema(description = "会员充值记录")
public class RechargeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "充值记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "会员ID", example = "1")
    private Long memberId;

    @Schema(description = "充值金额（元）", example = "200.00")
    private BigDecimal amount;

    @Schema(description = "赠送金额（元）", example = "20.00")
    private BigDecimal giftAmount;

    @Schema(description = "支付方式：WECHAT=微信，ALIPAY=支付宝，CASH=现金", example = "WECHAT")
    private String paymentMethod;

    @Schema(description = "充值时间", example = "2026-07-09 10:00:00")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
