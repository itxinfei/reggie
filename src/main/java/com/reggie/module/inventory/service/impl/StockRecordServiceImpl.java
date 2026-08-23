package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.BatchFillHelper;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.StockRecordMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.StockRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 库存记录服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class StockRecordServiceImpl extends ServiceImpl<StockRecordMapper, StockRecord> implements StockRecordService {

    /** 食材服务 */
    @Autowired
    private MaterialService materialService;

    /** 食材Mapper（用于原子库存增减） */
    @Autowired
    private MaterialMapper materialMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockIn(Long materialId, BigDecimal qty, BigDecimal unitPrice, Long bizId, String remark, String operator) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("入库数量必须大于0");
        }
        // 修改点：原子增加库存，消除 read-check-write 并发丢失更新
        int rows = materialMapper.addStock(materialId, qty);
        if (rows == 0) {
            throw new CustomException("食材不存在");
        }

        StockRecord record = new StockRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setMaterialId(materialId);
        record.setType(StockRecordType.IN.getValue());
        record.setQty(qty);
        record.setQuantity(qty);
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
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("出库数量必须大于0");
        }
        // 修改点：原子扣减库存（WHERE stock_qty >= #{qty} 由 SQL 保证），消除 read-check-write 超卖
        int rows = materialMapper.deductStock(materialId, qty);
        if (rows == 0) {
            // 0 行表示食材不存在或库存不足，统一提示库存不足
            throw new CustomException("库存不足");
        }

        StockRecord record = new StockRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setMaterialId(materialId);
        record.setType(StockRecordType.OUT.getValue());
        record.setQty(qty);
        record.setQuantity(qty);
        record.setBizId(bizId);
        record.setRemark(remark);
        record.setOperator(operator);
        save(record);
    }

    @Override
    public Page<StockRecord> pageByMaterial(Long materialId, int page, int pageSize) {
        Page<StockRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(StockRecord::getMaterialId, materialId);
        qw.orderByDesc(StockRecord::getCreatedTime);
        Page<StockRecord> result = page(pageInfo, qw);
        List<StockRecord> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillMaterialName(records);
        }
        return result;
    }

    public Page<StockRecord> page(Page<StockRecord> pageInfo) {
        Page<StockRecord> result = super.page(pageInfo);
        List<StockRecord> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillMaterialName(records);
        }
        return result;
    }

    /**
     * 重写带条件分页（Controller 实际调用此重载），在父类分页结果上回填物料名称与 quantity
     * 注意：IService.page 为泛型方法 <E extends IPage<T>>，子类必须以相同泛型签名重写，否则擦除冲突。
     */
    @Override
    public <E extends IPage<StockRecord>> E page(E page, Wrapper<StockRecord> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<StockRecord> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillMaterialName(records);
        }
        return result;
    }

    @Override
    public List<StockRecord> list(Wrapper<StockRecord> queryWrapper) {
        List<StockRecord> list = super.list(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            fillMaterialName(list);
        }
        return list;
    }

    /**
     * 批量填充库存记录的物料名称，并为 quantity 赋值（与 qty 一致）
     */
    private void fillMaterialName(List<StockRecord> records) {
        if (CollectionUtils.isEmpty(records)) return;

        // 同步 quantity = qty
        for (StockRecord r : records) {
            r.setQuantity(r.getQty());
        }

        BatchFillHelper.fillNames(
                records,
                StockRecord::getMaterialId,
                ids -> materialService.list(new LambdaQueryWrapper<Material>().in(Material::getId, ids))
                        .stream().collect(Collectors.toMap(Material::getId, Material::getName, (v1, v2) -> v1)),
                StockRecord::setMaterialName);
    }
}

