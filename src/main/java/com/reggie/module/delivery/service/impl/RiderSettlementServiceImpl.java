package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.mapper.RiderAccountMapper;
import com.reggie.module.delivery.mapper.RiderIncomeLedgerMapper;
import com.reggie.module.delivery.mapper.RiderWithdrawalMapper;
import com.reggie.module.delivery.model.RiderAccount;
import com.reggie.module.delivery.model.RiderIncomeLedger;
import com.reggie.module.delivery.model.RiderWithdrawal;
import com.reggie.module.delivery.service.RiderSettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 骑手结算与提现服务实现（与会员余额解耦）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Service
public class RiderSettlementServiceImpl implements RiderSettlementService {

    private static final Logger log = LoggerFactory.getLogger(RiderSettlementServiceImpl.class);

    @Autowired
    private RiderAccountMapper accountMapper;

    @Autowired
    private RiderIncomeLedgerMapper ledgerMapper;

    @Autowired
    private RiderWithdrawalMapper withdrawalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settle(Long orderId, Long riderId, Long tenantId, BigDecimal deliveryFee) {
        if (deliveryFee == null || deliveryFee.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        RiderIncomeLedger ledger = new RiderIncomeLedger();
        ledger.setTenantId(tenantId);
        ledger.setRiderId(riderId);
        ledger.setOrderId(orderId);
        ledger.setAmount(deliveryFee);
        ledger.setStatus(1);
        ledger.setCreateTime(LocalDateTime.now());
        try {
            ledgerMapper.insert(ledger);
        } catch (DuplicateKeyException e) {
            // 该订单已入账（重试/并发），跳过，避免重复结算
            log.info("订单已结算，跳过重复入账：orderId={}", orderId);
            return;
        }
        accountMapper.upsertBalance(riderId, tenantId, deliveryFee);
        log.info("骑手入账：orderId={}, riderId={}, amount={}", orderId, riderId, deliveryFee);
    }

    @Override
    public RiderAccount getAccount(Long riderId, Long tenantId) {
        RiderAccount account = accountMapper.selectByRiderTenant(riderId, tenantId);
        if (account == null) {
            account = new RiderAccount();
            account.setRiderId(riderId);
            account.setTenantId(tenantId);
            account.setWithdrawableBalance(BigDecimal.ZERO);
            account.setFrozenBalance(BigDecimal.ZERO);
            account.setTotalIncome(BigDecimal.ZERO);
            account.setTotalWithdrawn(BigDecimal.ZERO);
        }
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderWithdrawal applyWithdraw(Long riderId, Long tenantId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("提现金额必须大于 0");
        }
        int rows = accountMapper.freeze(riderId, tenantId, amount);
        if (rows == 0) {
            throw new CustomException("可提现余额不足");
        }
        RiderWithdrawal w = new RiderWithdrawal();
        w.setTenantId(tenantId);
        w.setRiderId(riderId);
        w.setAmount(amount);
        w.setStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();
        w.setApplyTime(now);
        w.setCreateTime(now);
        w.setUpdateTime(now);
        withdrawalMapper.insert(w);
        log.info("骑手提交提现：riderId={}, amount={}", riderId, amount);
        return w;
    }

    @Override
    public List<RiderWithdrawal> myWithdrawals(Long riderId, Long tenantId) {
        LambdaQueryWrapper<RiderWithdrawal> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderWithdrawal::getRiderId, riderId)
                .eq(RiderWithdrawal::getTenantId, tenantId)
                .orderByDesc(RiderWithdrawal::getApplyTime);
        return withdrawalMapper.selectList(qw);
    }

    @Override
    public Page<RiderWithdrawal> listWithdrawals(Long tenantId, String status, int page, int size) {
        Page<RiderWithdrawal> p = PageUtils.of(page, size);
        LambdaQueryWrapper<RiderWithdrawal> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderWithdrawal::getTenantId, tenantId);
        if (status != null && !status.trim().isEmpty()) {
            qw.eq(RiderWithdrawal::getStatus, status.trim());
        }
        qw.orderByDesc(RiderWithdrawal::getApplyTime);
        return withdrawalMapper.selectPage(p, qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderWithdrawal review(Long id, boolean approve, Long reviewerId, String reviewerName, String remark) {
        RiderWithdrawal w = withdrawalMapper.selectById(id);
        if (w == null) {
            throw new CustomException("提现申请不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(w.getTenantId())) {
            throw new CustomException("无权操作其他租户的提现申请");
        }
        if (!"PENDING".equals(w.getStatus())) {
            throw new CustomException("仅待审批状态的提现申请可审核");
        }
        // CAS 状态抢占：仅首个事务可流转状态并动账，避免并发双扣/双退
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<RiderWithdrawal>()
                .eq(RiderWithdrawal::getId, id)
                .eq(RiderWithdrawal::getStatus, "PENDING")
                .set(RiderWithdrawal::getStatus, approve ? "APPROVED" : "REJECTED")
                .set(RiderWithdrawal::getReviewTime, LocalDateTime.now())
                .set(RiderWithdrawal::getReviewerId, reviewerId)
                .set(RiderWithdrawal::getReviewerName, reviewerName)
                .set(RiderWithdrawal::getRemark, remark));
        if (claimed == 0) {
            throw new CustomException("提现申请状态已变更，请刷新后重试");
        }
        if (approve) {
            accountMapper.clearFrozen(w.getRiderId(), w.getTenantId(), w.getAmount());
        } else {
            accountMapper.returnFrozen(w.getRiderId(), w.getTenantId(), w.getAmount());
        }
        return withdrawalMapper.selectById(id);
    }
}
