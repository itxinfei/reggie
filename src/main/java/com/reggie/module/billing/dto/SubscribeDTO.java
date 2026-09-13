package com.reggie.module.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 发起订阅/续费请求
 *
 * @author reggie
 * @since 2026-09-12
 */
@Data
@Schema(description = "发起订阅/续费请求")
public class SubscribeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐ID", required = true, example = "1")
    @NotNull(message = "套餐ID不能为空")
    private Long planId;

    @Schema(description = "计费周期：1=月付，2=年付", required = true, example = "1")
    @NotNull(message = "计费周期不能为空")
    private Integer billingCycle;

    @Schema(description = "支付渠道：OFFLINE-线下转账，MOCK-模拟开通", example = "MOCK")
    private String payChannel;

    @Schema(description = "备注")
    private String remark;
}
