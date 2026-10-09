package com.reggie.module.subsidy.model;

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
 * 餐补账户实体（企业内部订餐）
 * <p>一个租户内一个顾客用户一个账户（uk_subsidy_tenant_user），
 * 余额变动一律经 {@code SubsidyService} 的原子 SQL + 流水记账，禁止直接改表。</p>
 */
@Data
@TableName("meal_subsidy_account")
@Schema(description = "餐补账户实体")
public class MealSubsidyAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "账户ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "顾客用户ID", example = "1")
    private Long userId;

    @Schema(description = "部门ID（发放时的归属部门快照）", example = "1")
    private Long departmentId;

    @Schema(description = "当前余额", example = "100.00")
    private BigDecimal balance;

    @Schema(description = "累计发放", example = "500.00")
    private BigDecimal totalGranted;

    @Schema(description = "累计核销", example = "400.00")
    private BigDecimal totalUsed;

    @Schema(description = "状态：0=冻结，1=正常", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic
    private Integer isDeleted;
}
