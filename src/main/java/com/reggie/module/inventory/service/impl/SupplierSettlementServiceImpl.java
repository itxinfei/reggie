package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供应商结算单服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
public class SupplierSettlementServiceImpl extends ServiceImpl<SupplierSettlementMapper, SupplierSettlement> implements
        SupplierSettlementService {

    /**
     * 分页查询 settlements。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param supplierId 参数 supplierId
     * @param status 参数 status
     * @return 返回结果
     */
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

    /**
     * 创建 settlement。
     * @param settlement 参数 settlement
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSettlement createSettlement(SupplierSettlement settlement) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        settlement.setTenantId(tenantId);
        settlement.setStatus("PENDING");
        settlement.setPaidAmount(settlement.getPaidAmount() == null ? java.math.BigDecimal.ZERO : settlement
                .getPaidAmount());
        settlement.setCreateTime(LocalDateTime.now());
        settlement.setUpdateTime(LocalDateTime.now());
        save(settlement);
        return settlement;
    }

    /**
     * 支付 settlement。
     * <p>P0-13 修复：totalAmount 判空防 compareTo NPE；超额付款拦截；
     * 已付金额与状态改用条件原子更新（WHERE 期望旧状态 + 余额上限条件），
     * 消除 getById→updateById 整行读改写的并发丢失更新与并发超额。</p>
     * @param id 参数 id
     * @param payAmount 参数 payAmount
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupplierSettlement paySettlement(Long id, BigDecimal payAmount) {
        SupplierSettlement settlement = getById(id);
        if (settlement == null) {
            throw new CustomException("结算单不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(settlement.getTenantId())) {
            throw new CustomException("无权操作其他租户的结算单");
        }
        if (!"PENDING".equals(settlement.getStatus())) {
            throw new CustomException("仅待付款结算单可付款");
        }
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("付款金额必须大于0");
        }
        if (settlement.getTotalAmount() == null) {
            throw new CustomException("结算单总金额未设置，无法付款");
        }
        BigDecimal totalAmount = settlement.getTotalAmount();
        BigDecimal paidBefore = settlement.getPaidAmount() == null ? BigDecimal.ZERO : settlement.getPaidAmount();
        BigDecimal remaining = totalAmount.subtract(paidBefore);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("结算单已付清，请勿重复付款");
        }
        if (payAmount.compareTo(remaining) > 0) {
            throw new CustomException("付款金额不能超过未付余额 " + remaining.toPlainString() + " 元");
        }
        boolean fullPayment = paidBefore.add(payAmount).compareTo(totalAmount) >= 0;

        // 条件原子更新：状态仍为 PENDING 且累加后不超过总额才生效；
        // 并发第二笔付款条件不满足 → 影响行数 0 → 抛错，杜绝丢失更新与超额付款
        LambdaUpdateWrapper<SupplierSettlement> uw = new LambdaUpdateWrapper<>();
        uw.eq(SupplierSettlement::getId, id)
                .eq(SupplierSettlement::getStatus, "PENDING")
                .apply("COALESCE(paid_amount, 0) + {0} <= COALESCE(total_amount, 0)", payAmount)
                .set(SupplierSettlement::getUpdateTime, LocalDateTime.now());
        // payAmount 已校验为非空正数 BigDecimal，toPlainString 无注入风险
        uw.setSql("paid_amount = COALESCE(paid_amount, 0) + " + payAmount.toPlainString()
                + (fullPayment ? ", status = 'PAID'" : ""));
        boolean updated = update(uw);
        if (!updated) {
            throw new CustomException("结算单状态或金额已变更，请刷新后重试");
        }
        return getById(id);
    }
}
