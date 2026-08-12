package com.reggie.module.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.category.model.Category;

/**
 * <p>
 * 分类管理服务接口，提供菜品分类和套餐分类的增删改查功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface CategoryService extends IService<Category> {

    /**
     * 删除分类，删除前校验是否关联了菜品或套餐
     *
     * @param id 分类ID
     */
    public void remove(Long id);

}

