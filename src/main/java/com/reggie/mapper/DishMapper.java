package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * <p>
 * 菜品 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 原子扣减菜品库存：stock_qty = stock_qty - #{qty}，WHERE stock_qty >= #{qty} 防止扣成负数
     * 修改点：参数化 @Update 替代 LambdaUpdateWrapper.setSql 字符串拼接
     */
    @Update("UPDATE dish SET stock_qty = stock_qty - #{qty}, updated_time = NOW() " +
            "WHERE id = #{id} AND stock_qty >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * 原子增加菜品库存：stock_qty = IFNULL(stock_qty, 0) + #{qty}
     * 修改点：参数化 @Update 替代字符串拼接，用于取消订单/补偿时回退库存
     */
    @Update("UPDATE dish SET stock_qty = IFNULL(stock_qty, 0) + #{qty}, updated_time = NOW() " +
            "WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
