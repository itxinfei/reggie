package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.StockRecord;
import java.math.BigDecimal;

/**
 * 库存流水记录服务接口
 * 记录原料的出库、入库流水
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface StockRecordService extends IService<StockRecord> {

    /**
     * 入库操作（增加库存）
     *
     * @param materialId 原料ID
     * @param qty        入库数量
     * @param unitPrice  单价
     * @param bizId      关联业务ID（如采购订单ID）
     * @param remark     备注
     * @param operator   操作人
     */
    void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId, String remark, String operator);

    /**
     * 出库操作（减少库存）
     *
     * @param materialId 原料ID
     * @param qty        出库数量
     * @param bizId      关联业务ID
     * @param remark     备注
     * @param operator   操作人
     */
    void stockOut(Long materialId, BigDecimal qty, Long bizId, String remark, String operator);

    /**
     * 分页查询指定原料的库存流水
     *
     * @param materialId 原料ID
     * @param page       页码
     * @param pageSize   每页条数
     * @return 分页流水记录
     */
    Page<StockRecord> pageByMaterial(Long materialId, int page, int pageSize);
}
