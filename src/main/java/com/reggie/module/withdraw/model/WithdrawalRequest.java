package com.reggie.module.withdraw.model;

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
 * 提现申请
 *
 * @author reggie
 * @since 2026-09-01
 */
@Data
@TableName("withdrawal_request")
@Schema(description = "提现申请")
public class WithdrawalRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "提现申请ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "提现金额（元）", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "银行名称", example = "中国工商银行")
    private String bankName;

    @Schema(description = "开户人姓名", example = "张三")
    private String accountName;

    @Schema(description = "银行账号", example = "622202xxxxxxxxxx")
    private String accountNumber;

    @Schema(description = "状态：PENDING=待审批，APPROVED=已同意，REJECTED=已拒绝", example = "PENDING")
    private String status;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "创建时间", example = "2026-09-01 10:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "审批时间", example = "2026-09-01 12:00:00")
    private LocalDateTime approveTime;

    @Schema(description = "审批人ID", example = "1")
    private Long approveUserId;

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
