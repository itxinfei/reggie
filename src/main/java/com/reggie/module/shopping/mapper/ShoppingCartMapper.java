package com.reggie.module.shopping.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.shopping.model.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 购物车 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    /**
     * 原子减 1：number = number - 1，WHERE id = ? AND number > 1
     * 修改点：参数化 @Update 替代 setSql 字符串拼接
     * @return 受影响行数
     */
    @Update("UPDATE shopping_cart SET number = number - 1 " +
            "WHERE id = #{id} AND number > 1")
    int subQuantity(@Param("id") Long id);

    /**
     * 原子加：number = number + #{increment}，WHERE id = ?
     * 修改点：参数化 @Update 替代 setSql 字符串拼接
     * @return 受影响行数
     */
    @Update("UPDATE shopping_cart SET number = number + #{increment} " +
            "WHERE id = #{id}")
    int addQuantity(@Param("id") Long id, @Param("increment") int increment);
}

