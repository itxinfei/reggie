package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 配送订单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {
}
