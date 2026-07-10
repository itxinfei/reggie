package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * 购物车商品数量减一
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

        ShoppingCart cartItem = this.getOne(wrapper);
        if (cartItem != null) {
            if (cartItem.getNumber() > 1) {
                cartItem.setNumber(cartItem.getNumber() - 1);
                this.updateById(cartItem);
                return cartItem;
            } else {
                this.removeById(cartItem.getId());
                return null;
            }
        }
        return null;
    }
}
