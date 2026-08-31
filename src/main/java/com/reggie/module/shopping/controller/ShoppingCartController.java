package com.reggie.module.shopping.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.enums.DishStatus;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.shopping.service.ShoppingCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/shopping-cart")
@Tag(name = "购物车管理", description = "购物车CRUD接口")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 添加购物车
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "添加到购物车", description = "添加商品到购物车")
    @Parameter(name = "shoppingCart", description = "购物车信息", required = true)
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据：userId={}, dishId={}, dishName={}, number={}",
            shoppingCart.getUserId(),
            shoppingCart.getDishId() != null ? shoppingCart.getDishId() : shoppingCart.getSetmealId(),
            shoppingCart.getName(),
            shoppingCart.getNumber());

        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();
        Long currentTenantId = BaseContext.getCurrentTenantId();

        // 从服务端获取菜品/套餐真实价格（防止客户端篡改）；
        // 幽灵菜品拦截：不存在/已停售/跨租户的菜品或套餐一律拒绝加购，禁止保留客户端金额入库
        if (dishId != null) {
            Dish dish = dishService.getById(dishId);
            if (dish == null) {
                throw new CustomException("菜品不存在，无法加入购物车");
            }
            if (currentTenantId != null && !currentTenantId.equals(dish.getTenantId())) {
                throw new CustomException("无权使用其他门店的菜品");
            }
            if (dish.getStatus() == null || dish.getStatus() != DishStatus.ENABLED.getValue()) {
                throw new CustomException("菜品「" + dish.getName() + "」已停售，无法加入购物车");
            }
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        } else if (shoppingCart.getSetmealId() != null) {
            Setmeal setmeal = setmealService.getById(shoppingCart.getSetmealId());
            if (setmeal == null) {
                throw new CustomException("套餐不存在，无法加入购物车");
            }
            if (currentTenantId != null && !currentTenantId.equals(setmeal.getTenantId())) {
                throw new CustomException("无权使用其他门店的套餐");
            }
            if (setmeal.getStatus() == null || setmeal.getStatus() != DishStatus.ENABLED.getValue()) {
                throw new CustomException("套餐「" + setmeal.getName() + "」已停用，无法加入购物车");
            }
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        } else {
            throw new CustomException("缺少菜品或套餐ID，无法加入购物车");
        }

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);

        }else{
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中
        //SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if(cartServiceOne != null){
            // 原子加 1 后重新查询最新数据（避免本地对象与 DB 不一致）
            int updated = shoppingCartService.addQuantityAtomically(cartServiceOne.getId(), 1);
            if (updated > 0) {
                cartServiceOne = shoppingCartService.getById(cartServiceOne.getId());
            } else {
                // 并发冲突，重新查询
                cartServiceOne = shoppingCartService.getById(cartServiceOne.getId());
            }
        }else{
            //如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询购物车", description = "查看当前用户的购物车列表")
    public R<List<ShoppingCart>> list(){
        log.info("查看购物车...");

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    /**
     * 减商品（使用原子操作防止并发竞态）
     * @param shoppingCart
     * @return
     */
    @PostMapping("/sub")
    @Operation(summary = "减少购物车商品", description = "减少购物车中商品的数量")
    @Parameter(name = "shoppingCart", description = "购物车信息", required = true)
    public R<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart) {
        // 先查找到购物车项
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        if (shoppingCart.getDishId() != null) {
            wrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else if (shoppingCart.getSetmealId() != null) {
            wrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        } else {
            return R.error("缺少菜品或套餐ID");
        }

        ShoppingCart cartItem = shoppingCartService.getOne(wrapper);
        if (cartItem == null) {
            return R.error("购物车商品不存在");
        }

        // 使用原子操作减一，防止并发竞态
        int affected = shoppingCartService.subQuantityAtomically(cartItem.getId());
        if (affected > 0) {
            // 重新查询最新数量
            cartItem = shoppingCartService.getById(cartItem.getId());
            return R.success(cartItem);
        } else {
            // 数量已为1，删除后返回null
            shoppingCartService.removeById(cartItem.getId());
            return R.success(null);
        }
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清空购物车", description = "清空当前用户的购物车")
    public R<String> clean(){
        //SQL:delete from shopping_cart where user_id = ?

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空购物车成功");
    }
}




