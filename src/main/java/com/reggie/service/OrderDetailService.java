package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.OrderDetail;

import java.util.List;

/**
 * <p>
 * 订单明细服务接口，提供订单明细数据的增删改查功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface OrderDetailService extends IService<OrderDetail> {

    /**
     * 根据订单ID查询明细列表
     *
     * @param orderId 订单ID
     * @return 明细列表
     */
    List<OrderDetail> listByOrderId(Long orderId);
}
