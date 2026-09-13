package com.reggie.module.inventory.service;

import java.math.BigDecimal;

/**
 * 原料库存联动服务
 *
 * 当订单扣减菜品库存时，按 BOM 配方（dish_material 表）自动扣减原料库存；
 * 退款回退时同理恢复原料库存。
 *
 * @author reggie
 * @since 2026-09-12
 */
public interface MaterialStockService {

    /**
     * 按 BOM 配方扣减原料库存
     *
     * @param dishId  菜品ID
     * @param dishQty 菜品数量（正数）
     */
    void deductMaterialStock(Long dishId, BigDecimal dishQty);

    /**
     * 按 BOM 配方恢复原料库存
     *
     * @param dishId  菜品ID
     * @param dishQty 菜品数量（正数）
     */
    void restoreMaterialStock(Long dishId, BigDecimal dishQty);
}
