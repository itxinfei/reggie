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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加盟合同（含抽成规则）
 *
 * @author reggie
 * @since 2026-08-15
 */
@Data
@TableName("franchise_contract")
@Schema(description = "加盟合同")
public class FranchiseContract implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 抽成方式-按营业额比例 */
    public static final int COMMISSION_TYPE_RATE = 1;
    /** 抽成方式-固定金额/周期 */
    public static final int COMMISSION_TYPE_FIXED = 2;

    /** 结算周期-月结 */
    public static final int SETTLE_CYCLE_MONTHLY = 1;

    /** 合同状态-生效中 */
    public static final int STATUS_ACTIVE = 1;
    /** 合同状态-已终止 */
    public static final int STATUS_TERMINATED = 0;

    @Schema(description = "合同ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属总部租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "加盟商ID", example = "1")
    @NotNull(message = "加盟商ID不能为空")
    private Long franchiseeId;

    @Schema(description = "加盟门店租户ID（store_info.tenant_id）", example = "2")
    private Long storeTenantId;

    @Schema(description = "合同编号", example = "FR-2026-0001")
    @NotBlank(message = "合同编号不能为空")
    private String contractNo;

    @Schema(description = "合同开始日期")
    private LocalDate startDate;

    @Schema(description = "合同结束日期")
    private LocalDate endDate;

    @Schema(description = "抽成方式：1=按营业额比例，2=固定金额/周期", example = "1")
    @NotNull(message = "抽成方式不能为空")
    private Integer commissionType;

    @Schema(description = "抽成比例（commission_type=1时使用，如0.0500=5%）", example = "0.0500")
    private BigDecimal commissionRate;

    @Schema(description = "固定抽成金额/周期（commission_type=2时使用）", example = "3000.00")
    private BigDecimal commissionAmount;

    @Schema(description = "结算周期：1=月结", example = "1")
    private Integer settleCycle;

    @Schema(description = "合同状态：0=已终止，1=生效中", example = "1")
    private Integer status;

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
