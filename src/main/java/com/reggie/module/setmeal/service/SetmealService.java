package com.reggie.module.setmeal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.SetmealDto;
import com.reggie.module.setmeal.model.Setmeal;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 套餐管理服务接口，提供套餐的增删改查及菜品关联管理功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     *
     * @param setmealDto 套餐及菜品关联信息
     */
    public void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     *
     * @param ids 套餐ID列表
     */
    public void removeWithDish(List<Long> ids);

    /**
     * 根据ID查询套餐信息及关联的菜品列表
     *
     * @param id 套餐ID
     * @return 套餐及菜品关联信息
     */
    public SetmealDto getByIdWithDish(Long id);

    /**
     * 更新套餐信息，同时更新套餐和菜品的关联关系
     *
     * @param setmealDto 套餐及菜品关联信息
     */
    public void updateWithDish(SetmealDto setmealDto);

    /**
     * 批量修改套餐起售停售状态
     *
     * @param status 目标状态（0停售 1起售）
     * @param ids 套餐ID列表
     */
    public void updateStatus(Integer status, List<Long> ids);

    /**
     * 获取套餐统计数据
     *
     * @return 统计数据（total/active/inactive）
     */
    public Map<String, Object> getStats();
}

