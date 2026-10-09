package com.reggie.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.order.model.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单明细 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}

