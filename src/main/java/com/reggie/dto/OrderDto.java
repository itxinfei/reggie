package com.reggie.dto;

import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
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
public class OrderDto extends Orders {

    /**
     * 订单详情列表
     */
    private List<OrderDetail> orderDetails;
}
