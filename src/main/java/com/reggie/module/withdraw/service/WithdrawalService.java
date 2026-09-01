package com.reggie.module.withdraw.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.withdraw.model.WithdrawalRequest;
import com.reggie.module.withdraw.model.WithdrawalRecord;

import java.math.BigDecimal;

/**
 * <p>
 * 提现服务接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
public interface WithdrawalService extends IService<WithdrawalRequest> {

    /**
     * 用户提交提现申请
     */
    WithdrawalRequest submitWithdrawal(WithdrawalRequest request);

    /**
     * 管理员同意提现
     */
    WithdrawalRequest approveWithdrawal(Long id, Long approveUserId);

    /**
     * 管理员拒绝提现
     */
    WithdrawalRequest rejectWithdrawal(Long id, String rejectReason, Long approveUserId);

    /**
     * 分页查询提现申请
     */
    Page<WithdrawalRequest> listWithdrawals(int page, int pageSize, String status);

    /**
     * 确认转账完成（生成提现记录）
     */
    WithdrawalRecord confirmTransfer(Long id, BigDecimal actualAmount, BigDecimal fee, String bankTraceNo);
}
