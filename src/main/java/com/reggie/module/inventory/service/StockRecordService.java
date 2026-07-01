package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.StockRecord;
import java.math.BigDecimal;

public interface StockRecordService extends IService<StockRecord> {
    void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId, String remark, String operator);
    void stockOut(Long materialId, BigDecimal qty, Long bizId, String remark, String operator);
    Page<StockRecord> pageByMaterial(Long materialId, int page, int pageSize);
}
