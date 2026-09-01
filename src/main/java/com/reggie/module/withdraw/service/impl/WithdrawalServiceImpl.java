package com.reggie.module.withdraw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.withdraw.mapper.WithdrawalRecordMapper;
import com.reggie.module.withdraw.mapper.WithdrawalRequestMapper;
import com.reggie.module.withdraw.model.WithdrawalRecord;
import com.reggie.module.withdraw.model.WithdrawalRequest;
import com.reggie.module.withdraw.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
public class WithdrawalServiceImpl extends ServiceImpl<WithdrawalRequestMapper, WithdrawalRequest> implements WithdrawalService {

    @Autowired
    private WithdrawalRecordMapper withdrawalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRequest submitWithdrawal(WithdrawalRequest request) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        request.setTenantId(tenantId);
        request.setStatus("PENDING");
        request.setCreateTime(LocalDateTime.now());
        save(request);
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRequest approveWithdrawal(Long id, Long approveUserId) {
        WithdrawalRequest exist = getById(id);
        if (exist == null) {
            throw new CustomException("提现申请不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的提现申请");
        }
        if (!"PENDING".equals(exist.getStatus())) {
            throw new CustomException("仅待审批状态的提现申请可审批");
        }
        exist.setStatus("APPROVED");
        exist.setApproveTime(LocalDateTime.now());
        exist.setApproveUserId(approveUserId);
        updateById(exist);
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRequest rejectWithdrawal(Long id, String rejectReason, Long approveUserId) {
        WithdrawalRequest exist = getById(id);
        if (exist == null) {
            throw new CustomException("提现申请不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的提现申请");
        }
        if (!"PENDING".equals(exist.getStatus())) {
            throw new CustomException("仅待审批状态的提现申请可审批");
        }
        exist.setStatus("REJECTED");
        exist.setRejectReason(rejectReason);
        exist.setApproveTime(LocalDateTime.now());
        exist.setApproveUserId(approveUserId);
        updateById(exist);
        return exist;
    }

    @Override
    public Page<WithdrawalRequest> listWithdrawals(int page, int pageSize, String status) {
        Page<WithdrawalRequest> pageRequest = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<WithdrawalRequest> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.trim().isEmpty()) {
            qw.eq(WithdrawalRequest::getStatus, status);
        }
        qw.orderByDesc(WithdrawalRequest::getCreateTime);
        return page(pageRequest, qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRecord confirmTransfer(Long id, BigDecimal actualAmount, BigDecimal fee, String bankTraceNo) {
        WithdrawalRequest exist = getById(id);
        if (exist == null) {
            throw new CustomException("提现申请不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的提现申请");
        }
        if (!"APPROVED".equals(exist.getStatus())) {
            throw new CustomException("仅已同意的提现申请可确认转账");
        }
        WithdrawalRecord record = new WithdrawalRecord();
        record.setTenantId(tenantId);
        record.setWithdrawalId(id);
        record.setActualAmount(actualAmount);
        record.setFee(fee);
        record.setTransferTime(LocalDateTime.now());
        record.setBankTraceNo(bankTraceNo);
        withdrawalRecordMapper.insert(record);
        return record;
    }
}
