package com.reggie.dto;

import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 订单数据传输对象
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单数据传输对象（含订单详情）")
public class OrderDto extends Orders {

    @Schema(description = "订单详情列表")
    private List<OrderDetail> orderDetails;
}
