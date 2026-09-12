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
public class PriceHistoryServiceImpl extends ServiceImpl<PriceHistoryMapper, PriceHistory> implements
        PriceHistoryService {

    @Autowired
    private PriceHistoryMapper priceHistoryMapper;

    /**
     * 处理 record price change。
     * @param materialId 参数 materialId
     * @param oldPrice 参数 oldPrice
     * @param newPrice 参数 newPrice
     * @param changeReason 参数 changeReason
     * @param operatorId 参数 operatorId
     * @return 返回结果
     */
    @Override
    public PriceHistory recordPriceChange(Long materialId, java.math.BigDecimal oldPrice, java.math.BigDecimal newPrice,
            String changeReason, Long operatorId) {
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

    /**
     * 查询列表 by material id。
     * @param materialId 参数 materialId
     * @return 返回结果
     */
    @Override
    public List<PriceHistory> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<PriceHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(PriceHistory::getMaterialId, materialId);
        qw.orderByDesc(PriceHistory::getCreateTime);
        return list(qw);
    }
}
