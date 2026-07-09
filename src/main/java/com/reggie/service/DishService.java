package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;

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
    public void saveDish(Dish dish, List<DishFlavor> flavors);

    //根据分类id查询菜品列表
    public List<Dish> listByCategoryId(Long categoryId);

    /**
     * 删除菜品及关联口味（事务保护），删除前校验套餐引用
     * 修改点：原Controller中for循环逐条删除无事务保护，改为Service层统一管理
     * @param ids 菜品ID列表
     */
    public void deleteWithFlavorCheck(List<Long> ids);
}
