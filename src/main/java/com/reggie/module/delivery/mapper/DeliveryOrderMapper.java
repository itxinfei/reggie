package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配送订单 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {
}
