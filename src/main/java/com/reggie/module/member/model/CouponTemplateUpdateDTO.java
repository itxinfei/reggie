package com.reggie.module.member.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 更新优惠券模板 DTO
 * <p>前端仅提交 id + 业务字段。tenantId 由服务端通过 BaseContext 校验归属，
 * 不允许前端指定。</p>
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
@Schema(description = "更新优惠券模板请求")
public class CouponTemplateUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID")
    @NotNull(message = "模板ID不能为空")
    private Long id;

    @Schema(description = "模板名称", example = "新人满减券")
    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @Schema(description = "优惠券类型")
    @NotBlank(message = "优惠券类型不能为空")
    private String type;

    @Schema(description = "满减条件金额（元）", example = "50.00")
    private BigDecimal conditionAmount;

    @Schema(description = "优惠金额（元）", example = "10.00")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率", example = "0.85")
    private BigDecimal discountRate;

    @Schema(description = "发放总数")
    @NotNull(message = "发放总数不能为空")
    @Min(value = 1, message = "发放总数必须大于0")
    private Integer totalCount;

    @Schema(description = "剩余可领数量")
    private Integer remainCount;

    @Schema(description = "有效天数")
    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于0")
    private Integer validDays;

    @Schema(description = "状态：0=禁用，1=启用")
    private Integer status;
}