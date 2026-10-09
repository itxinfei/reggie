package com.reggie.module.dining.vo;

import com.reggie.module.category.model.Category;
import com.reggie.module.dish.model.Dish;
import lombok.Data;

import java.util.List;

/**
 * 扫码点餐公开菜单：某桌台所属门店的菜品分类 + 在售菜品。
 * <p>租户由桌台反查确定（而非登录会话），供匿名顾客扫码后浏览，无需登录。</p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Data
public class DiningMenuVO {

    /** 菜品分类（type=1） */
    private List<Category> categories;

    /** 在售菜品 */
    private List<Dish> dishes;
}
