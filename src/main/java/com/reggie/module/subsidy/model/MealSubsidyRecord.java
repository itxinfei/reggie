package com.reggie.module.subsidy.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 餐补流水实体（企业内部订餐）
 * <p>类型：GRANT 发放 / CONSUME 核销 / REFUND 回充 / ADJUST 调整。
 * 幂等键 uk_subsidy_record_trade(trade_no, record_type)：
 * 核销用支付单 tradeNo，回充用退款单 refundNo，同一交易号同类型只允许一条流水。</p>
 */
@Data
@TableName("meal_subsidy_record")
@Schema(description = "餐补流水实体")
public class MealSubsidyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_GRANT = "GRANT";
    public static final String TYPE_CONSUME = "CONSUME";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_ADJUST = "ADJUST";

    @Schema(description = "流水ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "顾客用户ID", example = "1")
    private Long userId;

    @Schema(description = "账户ID", example = "1")
    private Long accountId;

    @Schema(description = "流水类型：GRANT/CONSUME/REFUND/ADJUST", example = "GRANT")
    private String recordType;

    @Schema(description = "发生金额（正向）", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "交易后余额", example = "100.00")
    private BigDecimal balanceAfter;

    @Schema(description = "关联订单ID（核销/回充）", example = "1")
    private Long orderId;

    @Schema(description = "关联交易号（幂等键）", example = "P20261006000001")
    private String tradeNo;

    @Schema(description = "操作人（管理员发放时）", example = "1")
    private Long operatorId;

    @Schema(description = "备注", example = "10月餐补")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
