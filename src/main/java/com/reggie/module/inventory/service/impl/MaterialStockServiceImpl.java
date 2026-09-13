package com.reggie.module.inventory.service.impl;

import com.reggie.module.inventory.mapper.MaterialMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.model.DishMaterial;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.service.DishMaterialService;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.service.MaterialStockService;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 原料库存联动服务实现
 *
 * 无 BOM 配方时静默跳过（log.debug），不阻断主流程；
 * 原料扣减失败时 log.error 但不抛异常，与菜品级库存回退的容错策略一致。
 *
 * @author reggie
 * @since 2026-09-12
 */
@Service
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class MaterialStockServiceImpl implements MaterialStockService {

    @Autowired(required = false)
    private DishMaterialService dishMaterialService;

    @Autowired
    private MaterialMapper materialMapper;

    @Override
    public void deductMaterialStock(Long dishId, BigDecimal dishQty) {
        if (dishId == null || dishQty == null || dishQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<DishMaterial> bomList = getBomList(dishId);
        if (bomList.isEmpty()) {
            log.debug("[原料扣减] 菜品ID={} 无BOM配方，跳过原料扣减", dishId);
            return;
        }
        for (DishMaterial bom : bomList) {
            BigDecimal totalQty = bom.getUsageQty().multiply(dishQty);
            if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                int affected = materialMapper.deductStock(bom.getMaterialId(), totalQty);
                if (affected == 0) {
                    log.warn("[原料扣减] 原料ID={} 库存不足或不存在，扣减{}失败（菜品ID={}）",
                            bom.getMaterialId(), totalQty, dishId);
                } else {
                    log.info("[原料扣减] 原料ID={} 扣减{}（菜品ID={} x {}）",
                            bom.getMaterialId(), totalQty, dishId, dishQty);
                }
            } catch (Exception e) {
                log.error("[原料扣减失败] 原料ID={} 扣减{}失败（菜品ID={}）: {}",
                        bom.getMaterialId(), totalQty, dishId, e.getMessage(), e);
            }
        }
    }

    @Override
    public void restoreMaterialStock(Long dishId, BigDecimal dishQty) {
        if (dishId == null || dishQty == null || dishQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<DishMaterial> bomList = getBomList(dishId);
        if (bomList.isEmpty()) {
            log.debug("[原料恢复] 菜品ID={} 无BOM配方，跳过原料恢复", dishId);
            return;
        }
        for (DishMaterial bom : bomList) {
            BigDecimal totalQty = bom.getUsageQty().multiply(dishQty);
            if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                materialMapper.addStock(bom.getMaterialId(), totalQty);
                log.info("[原料恢复] 原料ID={} 恢复{}（菜品ID={} x {}）",
                        bom.getMaterialId(), totalQty, dishId, dishQty);
            } catch (Exception e) {
                log.error("[原料恢复失败] 原料ID={} 恢复{}失败（菜品ID={}）: {}",
                        bom.getMaterialId(), totalQty, dishId, e.getMessage(), e);
            }
        }
    }

    /**
     * 获取菜品的 BOM 配方列表，DishMaterialService 未注入时返回空列表
     */
    private List<DishMaterial> getBomList(Long dishId) {
        if (dishMaterialService == null) {
            log.debug("[原料联动] DishMaterialService 未注入，跳过");
            return Collections.emptyList();
        }
        return dishMaterialService.listByDishId(dishId);
    }
}
