package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.inventory.mapper.StockRecordMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.StockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class StockRecordServiceImpl extends ServiceImpl<StockRecordMapper, StockRecord> implements StockRecordService {

    @Autowired
    private MaterialService materialService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId, String remark, String operator) {
        Material material = materialService.getById(materialId);
        if (material == null) {
            throw new CustomException("食材不存在");
        }
        material.setStockQty(material.getStockQty().add(qty));
        materialService.updateById(material);

        StockRecord record = new StockRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setMaterialId(materialId);
        record.setType(StockRecordType.IN.getValue());
        record.setQty(qty);
        record.setUnitPrice(unitPrice);
        record.setTotalAmount(unitPrice != null ? unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP) : null);
        record.setBizId(bizId);
        record.setRemark(remark);
        record.setOperator(operator);
        save(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockOut(Long materialId, BigDecimal qty, Long bizId, String remark, String operator) {
        Material material = materialService.getById(materialId);
        if (material == null) {
            throw new CustomException("食材不存在");
        }
        if (material.getStockQty().compareTo(qty) < 0) {
            throw new CustomException("库存不足");
        }
        material.setStockQty(material.getStockQty().subtract(qty));
        materialService.updateById(material);

        StockRecord record = new StockRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setMaterialId(materialId);
        record.setType(StockRecordType.OUT.getValue());
        record.setQty(qty);
        record.setBizId(bizId);
        record.setRemark(remark);
        record.setOperator(operator);
        save(record);
    }

    @Override
    public Page<StockRecord> pageByMaterial(Long materialId, int page, int pageSize) {
        Page<StockRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(StockRecord::getMaterialId, materialId);
        qw.orderByDesc(StockRecord::getCreatedTime);
        return page(pageInfo, qw);
    }
}
