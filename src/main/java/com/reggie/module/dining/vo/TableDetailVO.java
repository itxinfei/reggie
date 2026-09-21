package com.reggie.module.dining.vo;

import com.reggie.module.order.model.OrderDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 桌台明细 VO（收银台按桌查看订单 + 菜品明细）
 *
 * @author reggie
 * @since 2026-09-18
 */
@Data
@Schema(description = "桌台明细")
public class TableDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID")
    private Long tableId;

    @Schema(description = "桌台名称")
    private String tableName;

    @Schema(description = "区域名称")
    private String areaName;

    @Schema(description = "座位数")
    private Integer seatCount;

    @Schema(description = "桌台状态")
    private String status;

    @Schema(description = "用餐人数")
    private Integer customerCount;

    @Schema(description = "当前关联订单ID（结账/加菜依据，指向最早一笔活跃订单；无则为空）")
    private Long currentOrderId;

    @Schema(description = "该桌台下的订单列表")
    private List<TableOrder> orders;

    /**
     * 单个订单摘要
     */
    @Data
    @Schema(description = "桌台关联订单")
    public static class TableOrder implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "订单ID")
        private Long orderId;

        @Schema(description = "订单号")
        private String orderNumber;

        @Schema(description = "订单状态")
        private Integer status;

        @Schema(description = "订单金额")
        private BigDecimal amount;

        @Schema(description = "下单时间")
        private LocalDateTime orderTime;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "订单菜品明细")
        private List<OrderDetail> details;
    }
}
