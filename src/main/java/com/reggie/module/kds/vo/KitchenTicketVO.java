package com.reggie.module.kds.vo;

import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.order.model.OrderDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 后厨工单视图：工单本体 + 菜品明细 + 实时等待时长。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后厨工单视图")
public class KitchenTicketVO extends KitchenTicket {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品明细")
    private List<OrderDetail> details;

    @Schema(description = "等待时长（秒）：进行中为接单到当前，已叫号/完成为接单到叫号")
    private Long waitSeconds;
}
