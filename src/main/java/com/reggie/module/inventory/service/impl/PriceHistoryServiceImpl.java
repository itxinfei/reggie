package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.inventory.mapper.PriceHistoryMapper;
import com.reggie.module.inventory.model.PriceHistory;
import com.reggie.module.inventory.service.PriceHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格历史服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
public class PriceHistoryServiceImpl extends ServiceImpl<PriceHistoryMapper, PriceHistory> implements PriceHistoryService {

    @Autowired
    private PriceHistoryMapper priceHistoryMapper;

    @Override
    public PriceHistory recordPriceChange(Long materialId, java.math.BigDecimal oldPrice, java.math.BigDecimal newPrice, String changeReason, Long operatorId) {
        PriceHistory history = new PriceHistory();
        history.setTenantId(BaseContext.getCurrentTenantId());
        history.setMaterialId(materialId);
        history.setOldPrice(oldPrice);
        history.setNewPrice(newPrice);
        history.setChangeReason(changeReason);
        history.setOperatorId(operatorId);
        history.setCreateTime(LocalDateTime.now());
        save(history);
        return history;
    }

    @Override
    public List<PriceHistory> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<PriceHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(PriceHistory::getMaterialId, materialId);
        qw.orderByDesc(PriceHistory::getCreateTime);
        return list(qw);
    }
}
