package com.reggie.module.urgency.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 催单记录视图对象
 *
 * @author reggie
 * @since 2026-08-27
 */
@Data
@Schema(description = "催单记录")
public class UrgencyRecordVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "订单ID", example = "2001")
    private Long orderId;

    @Schema(description = "订单号", example = "ORD20260827001")
    private String orderNo;

    @Schema(description = "催单次数", example = "2")
    private Integer times;

    @Schema(description = "状态：SENT=已发送, PROCESSED=已处理, IGNORED=已忽略", example = "SENT")
    private String status;

    @Schema(description = "创建时间", example = "2026-08-27 10:30:00")
    private LocalDateTime createTime;
}
