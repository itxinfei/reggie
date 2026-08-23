package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.inventory.dto.DishMaterialBatchDTO;
import com.reggie.module.inventory.dto.DishMaterialSaveDTO;
import com.reggie.module.inventory.mapper.DishMaterialMapper;
import com.reggie.module.inventory.model.DishMaterial;
import com.reggie.module.inventory.service.DishMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜品食材关联 Service 实现
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DishMaterialServiceImpl extends ServiceImpl<DishMaterialMapper, DishMaterial>
        implements DishMaterialService {

    @Override
    public List<DishMaterial> listByDishId(Long dishId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        return baseMapper.listByDishId(dishId, tenantId);
    }

    @Override
    public void saveMaterial(DishMaterialSaveDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();

        // 校验菜品是否已绑定该食材（同一菜品+食材唯一）
        LambdaQueryWrapper<DishMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishMaterial::getDishId, dto.getDishId())
                .eq(DishMaterial::getMaterialId, dto.getMaterialId())
                .eq(DishMaterial::getTenantId, tenantId);
        long count = count(wrapper);
        if (count > 0) {
            throw new CustomException("该菜品已绑定该食材，请勿重复添加");
        }

        DishMaterial material = new DishMaterial();
        material.setDishId(dto.getDishId());
        material.setMaterialId(dto.getMaterialId());
        material.setUsageQty(dto.getUsageQty());
        material.setSort(dto.getSort() != null ? dto.getSort() : 0);
        material.setTenantId(tenantId);
        save(material);
        log.info("保存菜品食材关联，dishId={}, materialId={}, usageQty={}",
                dto.getDishId(), dto.getMaterialId(), dto.getUsageQty());
    }

    @Override
    public void updateMaterial(Long id, DishMaterialSaveDTO dto) {
        DishMaterial material = getById(id);
        if (material == null) {
            throw new CustomException("食材关联不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (!material.getTenantId().equals(tenantId)) {
            throw new CustomException("无权操作该租户的数据");
        }

        // 检查是否与其他已有记录冲突
        LambdaQueryWrapper<DishMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishMaterial::getDishId, dto.getDishId())
                .eq(DishMaterial::getMaterialId, dto.getMaterialId())
                .eq(DishMaterial::getTenantId, tenantId)
                .ne(DishMaterial::getId, id);
        long count = count(wrapper);
        if (count > 0) {
            throw new CustomException("该菜品已绑定该食材，请勿重复添加");
        }

        material.setDishId(dto.getDishId());
        material.setMaterialId(dto.getMaterialId());
        material.setUsageQty(dto.getUsageQty());
        material.setSort(dto.getSort() != null ? dto.getSort() : material.getSort());
        updateById(material);
        log.info("更新菜品食材关联，id={}", id);
    }

    @Override
    public void deleteMaterial(Long id) {
        DishMaterial material = getById(id);
        if (material == null) {
            throw new CustomException("食材关联不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (!material.getTenantId().equals(tenantId)) {
            throw new CustomException("无权操作该租户的数据");
        }
        removeById(id);
        log.info("删除菜品食材关联，id={}", id);
    }

    @Override
    public void batchSave(DishMaterialBatchDTO batchDTO) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long dishId = batchDTO.getDishId();

        List<DishMaterialSaveDTO> items = batchDTO.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new CustomException("食材明细不能为空");
        }

        // 先删除该菜品下所有关联
        baseMapper.deleteByDishId(dishId, tenantId);

        // 插入新关联
        List<DishMaterial> materials = new ArrayList<>();
        int sort = 0;
        for (DishMaterialSaveDTO item : items) {
            DishMaterial material = new DishMaterial();
            material.setDishId(dishId);
            material.setMaterialId(item.getMaterialId());
            material.setUsageQty(item.getUsageQty());
            material.setSort(item.getSort() != null ? item.getSort() : (sort++));
            material.setTenantId(tenantId);
            materials.add(material);
        }
        saveBatch(materials);
        log.info("批量保存菜品食材关联，dishId={}, count={}", dishId, materials.size());
    }
}
