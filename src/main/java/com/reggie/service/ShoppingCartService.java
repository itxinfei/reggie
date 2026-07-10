package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.ShoppingCart;

/**
 * 购物车服务接口，提供购物车的增删改查及数量调整功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 减少购物车中商品数量，数量为1时移除该条记录
     *
     * @param shoppingCart 购物车商品信息
     * @return 操作后的购物车信息
     */
    ShoppingCart sub(ShoppingCart shoppingCart);
}
