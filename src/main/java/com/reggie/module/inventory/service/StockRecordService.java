package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.StockRecord;
import java.math.BigDecimal;

/**
 * <p>
 * 库存流水记录服务接口
 * </p>
 * <p>记录原料的出库、入库流水</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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
    default void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId, String remark, String operator) {
        stockIn(materialId, qty, unitPrice, bizId, remark, operator, null);
    }

    /**
     * 入库操作并附带凭证图片。
     *
     * @param voucherImages 凭证图片（逗号分隔，可为 null）
     */
    void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId,
                  String remark, String operator, String voucherImages);

    /**
     * 出库操作（减少库存）
     *
     * @param materialId 原料ID
     * @param qty        出库数量
     * @param bizId      关联业务ID
     * @param remark     备注
     * @param operator   操作人
     */
    default void stockOut(Long materialId, BigDecimal qty, Long bizId, String remark, String operator) {
        stockOut(materialId, qty, bizId, remark, operator, null);
    }

    /**
     * 出库操作并附带凭证图片。
     *
     * @param voucherImages 凭证图片（逗号分隔，可为 null）
     */
    void stockOut(Long materialId, BigDecimal qty, Long bizId,
                   String remark, String operator, String voucherImages);

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
