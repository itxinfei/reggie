package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.entity.ShoppingCart;
import com.reggie.mapper.ShoppingCartMapper;
import com.reggie.service.ShoppingCartService;
import org.springframework.stereotype.Service;

/**
 * 购物车服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    /**
     * 购物车商品数量减一（使用原子操作防止竞态条件）
     *
     * @param shoppingCart 购物车项
     * @return 更新后的购物车项，数量为0时返回null
     */
    @Override
    public ShoppingCart sub(ShoppingCart shoppingCart) {
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        if (shoppingCart.getDishId() != null) {
            wrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else if (shoppingCart.getSetmealId() != null) {
            wrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        } else {
            // 既没有菜品ID也没有套餐ID，无法确定要减的商品
            return null;
        }

        // 先查询当前购物车项是否存在
        ShoppingCart cartItem = this.getOne(wrapper);
        if (cartItem == null) {
            return null;
        }

        if (cartItem.getNumber() > 1) {
            // 使用原子操作更新数量，防止并发问题
            int updated = getBaseMapper().subQuantity(cartItem.getId());
            if (updated > 0) {
                // 重新查询最新数量
                cartItem = this.getOne(wrapper);
                return cartItem;
            }
            // 更新失败（数量已变为1），重新查询
            cartItem = this.getOne(wrapper);
            if (cartItem != null && cartItem.getNumber() <= 1) {
                this.removeById(cartItem.getId());
                return null;
            }
            return cartItem;
        } else {
            // 数量为1，直接删除
            this.removeById(cartItem.getId());
            return null;
        }
    }

    /**
     * 原子操作：减少购物车商品数量（纯SQL原子更新，防止并发竞态）
     * 使用 UPDATE ... SET number = number - 1 WHERE id = ? AND number > 1
     * 返回受影响行数：1=成功，0=数量已为1或不存在
     *
     * @param itemId 购物车项ID
     * @return 受影响行数
     */
    @Override
    public int subQuantityAtomically(Long itemId) {
        if (itemId == null) {
            return 0;
        }
        return getBaseMapper().subQuantity(itemId);
    }

    /**
     * 原子操作：增加购物车商品数量
     * 使用 UPDATE ... SET number = number + 1 WHERE id = ?
     * 如果记录不存在返回 0，调用方需另行处理
     *
     * @param itemId 购物车项ID
     * @param increment 增加数量，默认为1
     * @return 受影响行数
     */
    @Override
    public int addQuantityAtomically(Long itemId, int increment) {
        if (itemId == null || increment <= 0) {
            return 0;
        }
        return getBaseMapper().addQuantity(itemId, increment);
    }
}
