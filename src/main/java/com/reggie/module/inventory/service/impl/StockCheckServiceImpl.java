package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.inventory.mapper.StockCheckMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.StockCheckService;
import com.reggie.module.inventory.service.StockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StockCheckServiceImpl extends ServiceImpl<StockCheckMapper, StockCheck> implements StockCheckService {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private StockRecordService stockRecordService;

    @Override
    public StockCheck createCheck(String operator, String remark) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
        qw.likeRight(StockCheck::getCheckNo, "CK" + datePrefix);
        qw.orderByDesc(StockCheck::getCheckNo).last("LIMIT 1");
        StockCheck last = getOne(qw);
        int seq = last != null ? Integer.parseInt(last.getCheckNo().substring(10)) + 1 : 1;

        StockCheck sc = new StockCheck();
        sc.setTenantId(BaseContext.getCurrentTenantId());
        sc.setCheckNo("CK" + datePrefix + String.format("%03d", seq));
        sc.setStatus("DRAFT");
        sc.setOperator(operator);
        sc.setRemark(remark);
        save(sc);
        return sc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCheck(Long checkId, List<Map<String, Object>> items) {
        StockCheck sc = getById(checkId);
        if (sc == null) {
            throw new CustomException("盘点单不存在");
        }
        if (!"DRAFT".equals(sc.getStatus()) && !"IN_PROGRESS".equals(sc.getStatus())) {
            throw new CustomException("盘点单状态不允许完成");
        }

        BigDecimal totalDiff = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            Long materialId = Long.valueOf(item.get("materialId").toString());
            BigDecimal actualQty = new BigDecimal(item.get("actualQty").toString());

            Material material = materialService.getById(materialId);
            if (material == null) {
                throw new CustomException("食材不存在: " + materialId);
            }

            BigDecimal diff = actualQty.subtract(material.getStockQty());
            totalDiff = totalDiff.add(diff.multiply(material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO));

            material.setStockQty(actualQty);
            materialService.updateById(material);

            StockRecord record = new StockRecord();
            record.setTenantId(BaseContext.getCurrentTenantId());
            record.setMaterialId(materialId);
            record.setType("CHECK");
            record.setQty(diff);
            record.setUnitPrice(material.getUnitPrice());
            record.setBizId(checkId);
            record.setRemark("盘点调整");
            record.setOperator(sc.getOperator());
            stockRecordService.save(record);
        }

        sc.setStatus("DONE");
        sc.setTotalDiffAmount(totalDiff.setScale(2, RoundingMode.HALF_UP));
        updateById(sc);
    }
}
