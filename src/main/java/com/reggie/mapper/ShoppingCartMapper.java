package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

/**
 * 购物车Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

}
