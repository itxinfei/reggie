package com.reggie.module.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 新客立减核价结果（内部核价对象）。
 *
 * @author reggie
 * @since 2026-09-24
 */
@Data
@Schema(description = "新客立减核价结果")
public class NewCustomerEvaluation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否命中新客立减")
    private boolean eligible;

    @Schema(description = "新客活动ID")
    private Long campaignId;

    @Schema(description = "活动名称")
    private String name;

    @Schema(description = "立减金额")
    private BigDecimal discountAmount;

    @Schema(description = "展示文案，如「新客立减 ¥8.00」")
    private String copyText;
}
