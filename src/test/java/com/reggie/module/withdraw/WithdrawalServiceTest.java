package com.reggie.module.withdraw;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.groupbuy.GroupBuyServiceTest;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import com.reggie.module.withdraw.model.WithdrawalRecord;
import com.reggie.module.withdraw.model.WithdrawalRequest;
import com.reggie.module.withdraw.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 提现服务单元测试
 * <p>覆盖：提交申请、审批通过（扣余额）、拒绝、确认转账、状态机校验。</p>
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema-member.sql", "classpath:schema-groupbuy-withdraw.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class WithdrawalServiceTest {

    @Autowired
    private WithdrawalService withdrawalService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private RechargeRecordService rechargeRecordService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void submitWithdrawal_valid_createsPending() {
        Member member = memberService.registerByPhone("13900139001", "提现测试");
        assertNotNull(member.getId());

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("500.00"));
        request.setBankName("工商银行");
        request.setAccountName("张三");
        request.setAccountNumber("6222021234567890");

        WithdrawalRequest saved = withdrawalService.submitWithdrawal(request);
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus());
        assertEquals(member.getId(), saved.getUserId());
        assertEquals(new BigDecimal("500.00"), saved.getAmount());
    }

    @Test
    void approveWithdrawal_success_deductsBalance() {
        // 创建会员并存入 1000 元
        Member member = memberService.registerByPhone("13900139002", "审批测试");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("1000.00"),
                BigDecimal.ZERO, "RECHARGE");

        // 提交提现申请
        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("300.00"));
        request.setBankName("建设银行");
        request.setAccountName("李四");
        request.setAccountNumber("6227001234567890");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        // 审批通过
        WithdrawalRequest approved = withdrawalService.approveWithdrawal(
                pending.getId(), 99L);
        assertEquals("APPROVED", approved.getStatus());
        assertNotNull(approved.getApproveTime());
        assertEquals(99L, approved.getApproveUserId());

        // 会员余额应扣减 300
        Member updated = memberService.getById(member.getId());
        assertEquals(new BigDecimal("700.00"), updated.getBalance());
    }

    @Test
    void approveWithdrawal_insufficientBalance_throws() {
        Member member = memberService.registerByPhone("13900139003", "余额不足测试");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("100.00"),
                BigDecimal.ZERO, "RECHARGE");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("500.00"));
        request.setBankName("农业银行");
        request.setAccountName("王五");
        request.setAccountNumber("6228481234567890");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        // 余额不足，应抛异常
        assertThrows(CustomException.class, () ->
                withdrawalService.approveWithdrawal(pending.getId(), 99L));

        // 状态应仍为 PENDING（事务回滚）
        WithdrawalRequest stillPending = withdrawalService.getById(pending.getId());
        assertEquals("PENDING", stillPending.getStatus());
        // 余额应未变动
        Member unchanged = memberService.getById(member.getId());
        assertEquals(new BigDecimal("100.00"), unchanged.getBalance());
    }

    @Test
    void rejectWithdrawal_valid_setsRejected() {
        Member member = memberService.registerByPhone("13900139004", "拒绝测试");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("200.00"));
        request.setBankName("中国银行");
        request.setAccountName("赵六");
        request.setAccountNumber("6212261234567890");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        WithdrawalRequest rejected = withdrawalService.rejectWithdrawal(
                pending.getId(), "材料不完整", 99L);
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("材料不完整", rejected.getRejectReason());
        assertNotNull(rejected.getApproveTime());
    }

    @Test
    void approveWithdrawal_nonPending_throws() {
        Member member = memberService.registerByPhone("13900139005", "非待审批测试");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setBankName("招商银行");
        request.setAccountName("孙七");
        request.setAccountNumber("6225881234567890");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        // 先拒绝
        withdrawalService.rejectWithdrawal(pending.getId(), "原因", 99L);

        // 再次审批应抛异常
        assertThrows(CustomException.class, () ->
                withdrawalService.approveWithdrawal(pending.getId(), 98L));
    }

    @Test
    void confirmTransfer_valid_marksTransferred() {
        Member member = memberService.registerByPhone("13900139006", "转账确认测试");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("500.00"),
                BigDecimal.ZERO, "RECHARGE");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("200.00"));
        request.setBankName("工商银行");
        request.setAccountName("周八");
        request.setAccountNumber("6222029876543210");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        // 审批通过
        withdrawalService.approveWithdrawal(pending.getId(), 99L);

        // 确认转账
        WithdrawalRecord record = withdrawalService.confirmTransfer(
                pending.getId(), new BigDecimal("195.00"), new BigDecimal("5.00"),
                "BNK202609010001");

        assertNotNull(record.getId());
        assertEquals(new BigDecimal("195.00"), record.getActualAmount());
        assertEquals(new BigDecimal("5.00"), record.getFee());
        assertEquals("BNK202609010001", record.getBankTraceNo());

        // 申请状态应流转为 TRANSFERRED
        WithdrawalRequest transferred = withdrawalService.getById(pending.getId());
        assertEquals("TRANSFERRED", transferred.getStatus());
    }

    @Test
    void confirmTransfer_nonApproved_throws() {
        Member member = memberService.registerByPhone("13900139007", "非已同意转账测试");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setBankName("建设银行");
        request.setAccountName("吴九");
        request.setAccountNumber("6227009876543210");
        WithdrawalRequest pending = withdrawalService.submitWithdrawal(request);

        // 未审批就直接确认转账应抛异常
        assertThrows(CustomException.class, () ->
                withdrawalService.confirmTransfer(pending.getId(),
                        new BigDecimal("100.00"), BigDecimal.ZERO, "TRACE001"));
    }

    @Test
    void listWithdrawals_findsPending() {
        Member member = memberService.registerByPhone("13900139008", "列表测试");

        WithdrawalRequest request = new WithdrawalRequest();
        request.setUserId(member.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setBankName("交通银行");
        request.setAccountName("郑十");
        request.setAccountNumber("6222621234567890");
        withdrawalService.submitWithdrawal(request);

        // 按 PENDING 状态过滤查询
        List<WithdrawalRequest> pendingList = withdrawalService.listWithdrawals(
                1, 10, "PENDING").getRecords();
        assertFalse(pendingList.isEmpty());
        assertTrue(pendingList.stream().anyMatch(r -> r.getId().equals(request.getId())));
    }
}
