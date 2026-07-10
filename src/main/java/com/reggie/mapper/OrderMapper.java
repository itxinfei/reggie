package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

}