package com.reggie.module.inventory.service.impl;

import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.StockRecordMapper;
import com.reggie.module.inventory.model.DishMaterial;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.DishMaterialService;
import com.reggie.module.inventory.service.MaterialStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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

    @Autowired(required = false)
    private StockRecordMapper stockRecordMapper;

    @Override
    public void deductMaterialStock(Long dishId, BigDecimal dishQty) {
        deductMaterialStock(dishId, dishQty, null);
    }

    @Override
    public void deductMaterialStock(Long dishId, BigDecimal dishQty, Long orderId) {
        if (dishId == null || dishQty == null || dishQty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<DishMaterial> bomList = getBomList(dishId);
        if (bomList.isEmpty()) {
            log.debug("[原料扣减] 菜品ID={} 无BOM配方，跳过原料扣减", dishId);
            return;
        }
        boolean shortage = false;
        for (DishMaterial bom : bomList) {
            BigDecimal totalQty = bom.getUsageQty().multiply(dishQty);
            if (totalQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            try {
                int affected = materialMapper.deductStock(bom.getMaterialId(), totalQty);
                if (affected == 0) {
                    // 库存不足未扣：升级 error 明确告警账实不符（不阻断营业——餐厅不应因库存录入滞后停卖），
                    // 便于运维/店长及时盘点补录；原仅 warn 容易被淹没
                    shortage = true;
                    log.error("[原料扣减] 原料ID={} 库存不足，应扣{}未扣（菜品ID={}），账实不符请盘点补录",
                            bom.getMaterialId(), totalQty, dishId);
                } else {
                    log.info("[原料扣减] 原料ID={} 扣减{}（菜品ID={} x {}）",
                            bom.getMaterialId(), totalQty, dishId, dishQty);
                    // P1-7：实际扣减成功才写订单流水（qty 记负值），未扣成功不写，保证流水与库存真实变动一致
                    writeStockRecord(bom.getMaterialId(), StockRecordType.SALE_ORDER,
                            totalQty.negate(), orderId);
                }
            } catch (Exception e) {
                log.error("[原料扣减失败] 原料ID={} 扣减{}失败（菜品ID={}）: {}",
                        bom.getMaterialId(), totalQty, dishId, e.getMessage(), e);
            }
        }
        if (shortage) {
            log.error("[原料扣减] 菜品ID={} 有原料缺货未扣全，订单照常但需尽快盘点补录", dishId);
        }
    }

    @Override
    public void restoreMaterialStock(Long dishId, BigDecimal dishQty) {
        restoreMaterialStock(dishId, dishQty, null);
    }

    @Override
    public void restoreMaterialStock(Long dishId, BigDecimal dishQty, Long orderId) {
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
                // P1-7：退款回补写流水（qty 正值）
                writeStockRecord(bom.getMaterialId(), StockRecordType.REFUND_ORDER,
                        totalQty, orderId);
            } catch (Exception e) {
                log.error("[原料恢复失败] 原料ID={} 恢复{}失败（菜品ID={}）: {}",
                        bom.getMaterialId(), totalQty, dishId, e.getMessage(), e);
            }
        }
    }

    /**
     * 写一条库存流水。失败仅告警不抛出——流水是追溯能力，不能因它阻断下单/退款主事务。
     */
    private void writeStockRecord(Long materialId, StockRecordType type, BigDecimal qty, Long orderId) {
        if (stockRecordMapper == null) {
            return;
        }
        try {
            StockRecord record = new StockRecord();
            record.setMaterialId(materialId);
            record.setType(type.getValue());
            record.setQty(qty);
            record.setBizId(orderId);
            record.setRemark(orderId == null ? type.getDesc() : type.getDesc() + "，订单ID=" + orderId);
            stockRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("[库存流水] 写流水失败：materialId={}, type={}, qty={}, orderId={}: {}",
                    materialId, type.getValue(), qty, orderId, e.getMessage(), e);
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
