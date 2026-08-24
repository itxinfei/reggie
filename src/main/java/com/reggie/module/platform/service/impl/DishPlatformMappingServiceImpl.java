package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.platform.mapper.DishPlatformMappingMapper;
import com.reggie.module.platform.model.DishPlatformMapping;
import com.reggie.module.platform.service.DishPlatformMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品平台映射服务实现
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Service
public class DishPlatformMappingServiceImpl extends ServiceImpl<DishPlatformMappingMapper, DishPlatformMapping> implements DishPlatformMappingService {

    @Override
    public List<DishPlatformMapping> listByDishId(Long dishId) {
        LambdaQueryWrapper<DishPlatformMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishPlatformMapping::getDishId, dishId)
                .eq(DishPlatformMapping::getIsDeleted, 0)
                .orderByDesc(DishPlatformMapping::getUpdateTime);
        return this.list(wrapper);
    }

    @Override
    public List<DishPlatformMapping> listByPlatformType(String platformType) {
        LambdaQueryWrapper<DishPlatformMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishPlatformMapping::getPlatformType, platformType)
                .eq(DishPlatformMapping::getIsDeleted, 0)
                .orderByDesc(DishPlatformMapping::getUpdateTime);
        return this.list(wrapper);
    }

    @Override
    public List<DishPlatformMapping> listByDishIdAndPlatformType(Long dishId, String platformType) {
        LambdaQueryWrapper<DishPlatformMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishPlatformMapping::getDishId, dishId)
                .eq(DishPlatformMapping::getPlatformType, platformType)
                .eq(DishPlatformMapping::getIsDeleted, 0);
        return this.list(wrapper);
    }

    @Override
    public DishPlatformMapping getByPlatformDishId(String platformType, String platformShopId, String platformDishId) {
        LambdaQueryWrapper<DishPlatformMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishPlatformMapping::getPlatformType, platformType)
                .eq(DishPlatformMapping::getPlatformShopId, platformShopId)
                .eq(DishPlatformMapping::getPlatformDishId, platformDishId)
                .eq(DishPlatformMapping::getIsDeleted, 0)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }
}
