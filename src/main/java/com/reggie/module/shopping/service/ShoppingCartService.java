package com.reggie.module.shopping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.shopping.model.ShoppingCart;

/**
 * <p>
 * 购物车服务接口，提供购物车的增删改查及数量调整功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface ShoppingCartService extends IService<ShoppingCart> {

    /**
     * 减少购物车中商品数量，数量为1时移除该条记录
     *
     * @param shoppingCart 购物车商品信息
     * @return 操作后的购物车信息
     */
    ShoppingCart sub(ShoppingCart shoppingCart);

    /**
     * 原子操作：减少购物车商品数量（纯SQL原子更新，防止并发竞态）
     * 使用 UPDATE ... SET number = number - 1 WHERE id = ? AND number > 1
     *
     * @param itemId 购物车项ID
     * @return 受影响行数：1=成功，0=数量已为1或不存在
     */
    int subQuantityAtomically(Long itemId);

    /**
     * 原子操作：增加购物车商品数量
     * 使用 UPDATE ... SET number = number + ? WHERE id = ?
     *
     * @param itemId 购物车项ID
     * @param increment 增加数量，默认为1
     * @return 受影响行数
     */
    int addQuantityAtomically(Long itemId, int increment);
}

