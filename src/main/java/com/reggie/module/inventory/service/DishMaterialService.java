package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.dto.DishMaterialBatchDTO;
import com.reggie.module.inventory.dto.DishMaterialSaveDTO;
import com.reggie.module.inventory.model.DishMaterial;

import java.util.List;

/**
 * 菜品食材关联 Service
 *
 * @author reggie
 * @since 2026-08-22
 */
public interface DishMaterialService extends IService<DishMaterial> {

    /**
     * 查询某菜品的全部食材配方
     *
     * @param dishId 菜品ID
     * @return 食材关联列表
     */
    List<DishMaterial> listByDishId(Long dishId);

    /**
     * 保存单个食材关联
     *
     * @param dto 保存DTO
     */
    void saveMaterial(DishMaterialSaveDTO dto);

    /**
     * 更新单个食材关联
     *
     * @param id  主键
     * @param dto 保存DTO
     */
    void updateMaterial(Long id, DishMaterialSaveDTO dto);

    /**
     * 删除单个食材关联
     *
     * @param id 主键
     */
    void deleteMaterial(Long id);

    /**
     * 批量保存某菜品的食材配方（先删后插，以最新提交为准）
     *
     * @param batchDTO 批量DTO
     */
    void batchSave(DishMaterialBatchDTO batchDTO);
}
