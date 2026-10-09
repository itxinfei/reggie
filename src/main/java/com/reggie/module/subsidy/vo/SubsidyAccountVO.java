package com.reggie.module.subsidy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 餐补账户 VO（管理端分页展示）
 * 附带顾客姓名/手机号/部门名，避免前端逐行反查。
 */
@Data
@Schema(description = "餐补账户VO")
public class SubsidyAccountVO {

    @Schema(description = "账户ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "顾客用户ID")
    private Long userId;

    @Schema(description = "顾客姓名")
    private String userName;

    @Schema(description = "顾客手机号（脱敏后）")
    private String phone;

    @Schema(description = "部门ID")
    private Long departmentId;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "当前余额")
    private BigDecimal balance;

    @Schema(description = "累计发放")
    private BigDecimal totalGranted;

    @Schema(description = "累计核销")
    private BigDecimal totalUsed;

    @Schema(description = "状态：0=冻结，1=正常")
    private Integer status;
}
