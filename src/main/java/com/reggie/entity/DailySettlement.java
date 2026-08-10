package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日结实例
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("daily_settlement")
@Schema(description = "日结")
public class DailySettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "结算日期")
    private LocalDate settlementDate;

    @Schema(description = "营业额")
    private BigDecimal totalRevenue;

    @Schema(description = "现金收入")
    private BigDecimal cashIncome;

    @Schema(description = "微信收入")
    private BigDecimal wechatIncome;

    @Schema(description = "支付宝收入")
    private BigDecimal alipayIncome;

    @Schema(description = "银行卡收入")
    private BigDecimal bankcardIncome;

    @Schema(description = "其他收入")
    private BigDecimal otherIncome;

    @Schema(description = "订单数量")
    private Integer orderCount;

    @Schema(description = "退款金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款数量")
    private Integer refundCount;

    @Schema(description = "净收入")
    private BigDecimal netIncome;

    @Schema(description = "食材成本")
    private BigDecimal materialCost;

    @Schema(description = "人工成本")
    private BigDecimal laborCost;

    @Schema(description = "其他成本")
    private BigDecimal otherCost;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "毛利润")
    private BigDecimal grossProfit;

    @Schema(description = "毛利率")
    private BigDecimal profitRate;

    @Schema(description = "结账状态：0-未结账，1-已结账")
    private Integer status;

    @Schema(description = "结账时间")
    private LocalDateTime settlementTime;

    @Schema(description = "结账人ID")
    private Long settlementUserId;

    @Schema(description = "结账人姓名")
    private String settlementUserName;

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
