package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新对应的口味信息
    public void updateWithFlavor(DishDto dishDto);

    public void updateStatus(Integer status, List<Long> ids);

    // 保存菜品及口味（事务保护）
    public void saveDish(Dish dish, List<com.reggie.entity.DishFlavor> flavors);

    //根据分类id查询菜品列表
    public List<Dish> listByCategoryId(Long categoryId);
}
