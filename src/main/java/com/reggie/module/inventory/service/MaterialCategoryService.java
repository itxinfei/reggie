package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.MaterialCategory;

/**
 * <p>
 * 原料分类服务接口
 * </p>
 * <p>管理原料分类信息（如蔬菜、肉类、调料等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface MaterialCategoryService extends IService<MaterialCategory> {

    /**
     * 分页查询原料分类
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页分类列表
     */
    Page<MaterialCategory> pageQuery(int page, int pageSize);
}

