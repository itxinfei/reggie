package com.reggie.dto;

import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * <p>
 * 订单数据传输对象
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单数据传输对象（含订单详情）")
public class OrderDto extends Orders {

    @Schema(description = "订单详情列表")
    private List<OrderDetail> orderDetails;
}

