package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.common.BaseContext;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.mapper.PriceHistoryMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.model.PriceHistory;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.inventory.service.PriceHistoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 价格历史服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
@Transactional(rollbackFor = Exception.class)
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
