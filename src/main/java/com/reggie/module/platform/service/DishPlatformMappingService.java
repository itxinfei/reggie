package com.reggie.module.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.platform.model.DishPlatformMapping;

import java.util.List;

/**
 * 商品平台映射服务接口
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface DishPlatformMappingService extends IService<DishPlatformMapping> {

    /**
     * 根据菜品 ID 查询所有映射
     */
    List<DishPlatformMapping> listByDishId(Long dishId);

    /**
     * 根据平台类型查询所有映射
     */
    List<DishPlatformMapping> listByPlatformType(String platformType);

    /**
     * 根据菜品 ID 和平台类型查询映射
     */
    List<DishPlatformMapping> listByDishIdAndPlatformType(Long dishId, String platformType);

    /**
     * 根据平台菜品 ID 查询映射
     */
    DishPlatformMapping getByPlatformDishId(String platformType, String platformShopId, String platformDishId);
}
