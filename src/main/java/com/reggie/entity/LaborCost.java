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
 * 人工成本实体
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("labor_cost")
@Schema(description = "人工成本")
public class LaborCost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "员工ID")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工资")
    private BigDecimal salary;

    @Schema(description = "社保")
    private BigDecimal socialInsurance;

    @Schema(description = "公积金")
    private BigDecimal housingFund;

    @Schema(description = "其他福利")
    private BigDecimal otherBenefits;

    @Schema(description = "总成本")
    private BigDecimal totalCost;

    @Schema(description = "成本月份")
    private LocalDate costMonth;

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
