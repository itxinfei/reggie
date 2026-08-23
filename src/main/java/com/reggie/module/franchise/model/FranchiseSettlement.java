package com.reggie.module.franchise.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 加盟分账结算单
 *
 * @author reggie
 * @since 2026-08-15
 */
@Data
@TableName("franchise_settlement")
@Schema(description = "加盟分账结算单")
public class FranchiseSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态-待确认 */
    public static final int STATUS_PENDING = 0;
    /** 状态-已确认 */
    public static final int STATUS_CONFIRMED = 1;
    /** 状态-已结算 */
    public static final int STATUS_SETTLED = 2;

    @Schema(description = "结算单ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属总部租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "加盟合同ID", example = "1")
    @NotNull(message = "加盟合同ID不能为空")
    private Long contractId;

    @Schema(description = "加盟商ID", example = "1")
    @NotNull(message = "加盟商ID不能为空")
    private Long franchiseeId;

    @Schema(description = "加盟门店租户ID", example = "2")
    private Long storeTenantId;

    @Schema(description = "结算周期，如 2026-08", example = "2026-08")
    @NotBlank(message = "结算周期不能为空")
    private String settlePeriod;

    @Schema(description = "周期内已完成订单数", example = "120")
    private Integer orderCount;

    @Schema(description = "周期营业额", example = "50000.00")
    private BigDecimal salesAmount;

    @Schema(description = "抽成方式（冗余合同快照）：1=比例，2=固定金额", example = "1")
    private Integer commissionType;

    @Schema(description = "抽成比例（冗余合同快照）", example = "0.0500")
    private BigDecimal commissionRate;

    @Schema(description = "应抽成金额", example = "2500.00")
    private BigDecimal commissionAmount;

    @Schema(description = "加盟商应结算金额（营业额-抽成）", example = "47500.00")
    private BigDecimal settleAmount;

    @Schema(description = "状态：0=待确认，1=已确认，2=已结算", example = "0")
    private Integer status;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
