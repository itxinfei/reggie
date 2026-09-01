package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.inventory.mapper.SupplierSettlementMapper;
import com.reggie.module.inventory.model.SupplierSettlement;
import com.reggie.module.inventory.service.SupplierSettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 供应商结算单服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
public class SupplierSettlementServiceImpl extends ServiceImpl<SupplierSettlementMapper, SupplierSettlement> implements SupplierSettlementService {

    @Override
    public Page<SupplierSettlement> pageSettlements(int page, int pageSize, Long supplierId, String status) {
        Page<SupplierSettlement> pageRequest = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<SupplierSettlement> qw = new LambdaQueryWrapper<>();
        if (supplierId != null) {
            qw.eq(SupplierSettlement::getSupplierId, supplierId);
        }
        if (status != null && !status.trim().isEmpty()) {
            qw.eq(SupplierSettlement::getStatus, status);
        }
        qw.orderByDesc(SupplierSettlement::getCreateTime);
        Page<SupplierSettlement> result = page(pageRequest, qw);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSettlement createSettlement(SupplierSettlement settlement) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        settlement.setTenantId(tenantId);
        settlement.setStatus("PENDING");
        settlement.setPaidAmount(settlement.getPaidAmount() == null ? java.math.BigDecimal.ZERO : settlement.getPaidAmount());
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        save(settlement);
        return settlement;
    }
}
