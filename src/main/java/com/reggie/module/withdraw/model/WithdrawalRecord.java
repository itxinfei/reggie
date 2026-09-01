package com.reggie.module.withdraw.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("withdrawal_record")
@Schema(description = "提现记录")
public class WithdrawalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "提现记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "提现申请ID", example = "1")
    private Long withdrawalId;

    @Schema(description = "实际到账金额（元）", example = "950.00")
    private BigDecimal actualAmount;

    @Schema(description = "手续费（元）", example = "50.00")
    private BigDecimal fee;

    @Schema(description = "转账时间", example = "2026-09-01 14:00:00")
    private LocalDateTime transferTime;

    @Schema(description = "银行流水号", example = "BN20260901001")
    private String bankTraceNo;
}
