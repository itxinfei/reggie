package com.reggie.module.withdraw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.reggie.module.member.model.Member;
import com.reggie.module.member.service.MemberService;
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
public class WithdrawalServiceImpl extends ServiceImpl<WithdrawalRequestMapper, WithdrawalRequest> implements
        WithdrawalService {

    @Autowired
    private WithdrawalRecordMapper withdrawalRecordMapper;

    /** 会员服务（审批通过时扣减提现金额） */
    @Autowired
    private MemberService memberService;

    /**
     * 单笔提现金额上限（元）：与 {@link WithdrawalRequest} 的 {@code @DecimalMax} 口径一致，
     * 服务端兜底二次校验（防绕过 Controller @Valid 直接调用 Service 的资金安全场景）。
     */
    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("50000.00");

    /**
     * 提交 withdrawal。
     * <p>资金安全校验（P0，2026-10-02 审查 §2.2-11）：金额非空/正数/单笔上限、申请金额不超当前可提现余额、
     * 申请人身份取当前会话用户（不采信客户端 userId）。原实现零校验，负额申请可在 approve 时经
     * {@code deductBalance} 反向加钱。</p>
     * @param request 参数 request
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WithdrawalRequest submitWithdrawal(WithdrawalRequest request) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        // 服务端金额兜底断言（不依赖 @Valid 注解路径）：必须 > 0 且不超单笔上限
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("提现金额必须大于0");
        }
        if (amount.compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new CustomException("提现金额不能超过" + MAX_WITHDRAWAL_AMOUNT.toPlainString() + "元");
        }
        // 申请人身份取当前会话用户，禁止采信客户端传入的 userId（防代他人提交/越权提现）
        Long applicantId = BaseContext.getCurrentId();
        if (applicantId == null) {
            throw new CustomException("登录状态异常，无法提交提现申请");
        }
        request.setUserId(applicantId);
        // 可提现余额校验：申请金额不得超过账户当前余额。账户口径与 approve 扣款一致
        // （MemberService.deductBalance 以 userId 作为会员ID定位账户），故此处同样按 userId 取会员余额。
        Member member = memberService.getById(applicantId);
        if (member == null) {
            throw new CustomException("会员账户不存在，无法申请提现");
        }
        BigDecimal balance = member.getBalance() == null ? BigDecimal.ZERO : member.getBalance();
        if (amount.compareTo(balance) > 0) {
            throw new CustomException("提现金额超过当前可提现余额");
        }
        request.setTenantId(tenantId);
        request.setStatus("PENDING");
        request.setCreateTime(LocalDateTime.now());
        save(request);
        return request;
    }

    /**
     * 审核通过 withdrawal。
     * @param id 参数 id
     * @param approveUserId 参数 approveUserId
     * @return 返回结果
     */
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
        // 兜底复核金额为正（P0 资金安全，2026-10-02 审查 §2.2-11）：负额/零额申请绝不允许进入扣款，
        // 防经 deductBalance 反向加钱。deductBalance 自身对 <=0 返回 false，但审批链前置断言不依赖下游实现细节，更稳妥。
        if (exist.getAmount() == null || exist.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("提现金额异常，无法审批通过");
        }
        // 审批通过即扣减会员余额：MemberService.deductBalance 内部用原子 SQL
        // (UPDATE member SET balance=balance-amount WHERE id=? AND balance>=amount) 防超扣；
        // 余额不足则扣款失败抛异常，@Transactional 回滚，提现状态不置 APPROVED。
        //
        // 并发防护（P0）：deductBalance 的 WHERE balance>=amount 只能防"单次扣超额"，
        // 无法防"双审批各扣一次"——若余额远大于提现金额，两个并发审批可先后各扣一次相同金额（双扣）。
        // 故审批状态流转必须用 CAS 条件更新抢占：仅 affected rows=1 的首个事务可扣款并置 APPROVED，
        // 其余并发事务 rows=0 直接抛错回滚，杜绝双扣。
        int claimed = baseMapper.update(null, new LambdaUpdateWrapper<WithdrawalRequest>()
                .eq(WithdrawalRequest::getId, id)
                .eq(WithdrawalRequest::getStatus, "PENDING")
                .set(WithdrawalRequest::getStatus, "APPROVED")
                .set(WithdrawalRequest::getApproveTime, LocalDateTime.now())
                .set(WithdrawalRequest::getApproveUserId, approveUserId));
        if (claimed == 0) {
            // CAS 抢占失败：该申请已被其他请求审批/拒绝，禁止重复扣款
            throw new CustomException("提现申请状态已变更，请刷新后重试");
        }
        boolean deducted = memberService.deductBalance(exist.getUserId(), exist.getAmount());
        if (!deducted) {
            throw new CustomException("会员余额不足，无法审批通过");
        }
        // 重新查询返回最新状态：此前的 `return exist` 是 CAS 前旧快照（status 仍为 PENDING），
        // 前端审批成功后却收到 PENDING，可能触发误判/重复提交，属真实生产缺陷。
        return getById(id);
    }

    /**
     * 驳回 withdrawal。
     * @param id 参数 id
     * @param rejectReason 参数 rejectReason
     * @param approveUserId 参数 approveUserId
     * @return 返回结果
     */
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
        // 兜底复核金额为正（P0 资金安全，2026-10-02 审查 §2.2-11）：异常金额申请在拒绝路径同样拦截，
        // 避免脏数据流入后续流程。
        if (exist.getAmount() == null || exist.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("提现金额异常，无法处理该申请");
        }
        exist.setStatus("REJECTED");
        exist.setRejectReason(rejectReason);
        exist.setApproveTime(LocalDateTime.now());
        exist.setApproveUserId(approveUserId);
        updateById(exist);
        return exist;
    }

    /**
     * 查询列表 withdrawals。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param status 参数 status
     * @return 返回结果
     */
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

    /**
     * 确认 transfer。
     * @param id 参数 id
     * @param actualAmount 参数 actualAmount
     * @param fee 参数 fee
     * @param bankTraceNo 参数 bankTraceNo
     * @return 返回结果
     */
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
        // 并发防护（P0）：先 CAS 抢占状态 APPROVED -> TRANSFERRED，仅受影响行数=1 的事务可创建转账记录。
        // 此前"SELECT 校验 -> insert 记录 -> updateById"两步无 CAS：并发确认转账时双请求都能通过
        // APPROVED 校验并各 insert 一条转账记录，形成重复转账。现条件更新互斥，第二个请求 rows=0 直接拒绝。
        int claimed = baseMapper.update(null, new LambdaUpdateWrapper<WithdrawalRequest>()
                .eq(WithdrawalRequest::getId, id)
                .eq(WithdrawalRequest::getStatus, "APPROVED")
                .set(WithdrawalRequest::getStatus, "TRANSFERRED"));
        if (claimed == 0) {
            throw new CustomException("提现申请状态已变更，请勿重复确认转账");
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
