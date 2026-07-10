package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

}
