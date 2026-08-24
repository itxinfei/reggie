package com.reggie.module.cashier.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收银记录实体
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("cashier_record")
@Schema(description = "收银记录")
public class CashierRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNumber;

    @Schema(description = "收银类型：1-现金，2-微信，3-支付宝，4-银行卡，5-会员储值")
    private Integer payType;

    @Schema(description = "收银金额")
    private BigDecimal amount;

    @Schema(description = "实收金额")
    private BigDecimal actualAmount;

    @Schema(description = "找零金额")
    private BigDecimal changeAmount;

    @Schema(description = "收银时间")
    private LocalDateTime cashierTime;

    @Schema(description = "收银员ID")
    private Long cashierId;

    @Schema(description = "收银员姓名")
    private String cashierName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private Long createUser;
}
