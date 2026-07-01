package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {
}
