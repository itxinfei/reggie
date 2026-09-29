package com.reggie.module.delivery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 骑手单笔收入明细视图对象。
 * <p>每一条对应一笔已完成配送订单的配送费收入与时效数据，供骑手端「我的收入」明细列表展示。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@Schema(description = "骑手单笔收入明细")
public class RiderIncomeRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单号", example = "202609280001")
    private String orderNumber;

    @Schema(description = "下单时间", example = "2026-09-28 12:30:00")
    private LocalDateTime orderTime;

    @Schema(description = "本单配送费收入（元）", example = "5.00")
    private BigDecimal income;

    @Schema(description = "配送里程（公里）", example = "2.35")
    private BigDecimal distance;

    @Schema(description = "实际送达时长（分钟）", example = "22")
    private Integer durationMin;

    @Schema(description = "是否准时送达", example = "true")
    private Boolean onTime;

    @Schema(description = "订单实收金额（元）", example = "48.00")
    private BigDecimal amount;
}
