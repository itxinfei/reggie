package com.reggie.module.member.service.impl;

import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.mapper.RechargeRecordMapper;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RechargeRecordServiceImpl#confirmRecharge 资金路径纯 Mockito 测试。
 * 用 spy 打桩 getByRechargeNo，聚焦 CAS 去重 → 原子入账 → 失败抛错的分支。
 *
 * @author reggie
 * @since 2026-09-21
 */
@ExtendWith(MockitoExtension.class)
class RechargeRecordConfirmTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    @Mock
    private MemberService memberService;

    private RechargeRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new RechargeRecordServiceImpl());
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "rechargeRecordMapper", rechargeRecordMapper);
        ReflectionTestUtils.setField(service, "memberService", memberService);
    }

    @Test
    void confirm_casReturns0_throwsAndNeverAddsBalance() {
        doReturn(buildRecord()).when(service).getByRechargeNo("RC1");
        when(rechargeRecordMapper.casConfirm(5L, 9L)).thenReturn(0);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.confirmRecharge("RC1", 9L));
        assertEqualsMsg("充值单已确认或已取消，请勿重复操作", ex);
        verify(memberMapper, never()).addBalance(any(), any(), any());
    }

    @Test
    void confirm_casThenAddBalance_ok() {
        doReturn(buildRecord()).when(service).getByRechargeNo("RC1");
        when(rechargeRecordMapper.casConfirm(5L, 9L)).thenReturn(1);
        when(memberMapper.addBalance(eq(10L), eq(new BigDecimal("100")), any())).thenReturn(1);

        service.confirmRecharge("RC1", 9L);

        verify(rechargeRecordMapper).casConfirm(5L, 9L);
        verify(memberMapper).addBalance(eq(10L), eq(new BigDecimal("100")), any());
    }

    @Test
    void confirm_addBalanceFails_throws() {
        doReturn(buildRecord()).when(service).getByRechargeNo("RC1");
        when(rechargeRecordMapper.casConfirm(5L, 9L)).thenReturn(1);
        when(memberMapper.addBalance(eq(10L), eq(new BigDecimal("100")), any())).thenReturn(0);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.confirmRecharge("RC1", 9L));
        assertEqualsMsg("会员不存在，入账失败", ex);
    }

    @Test
    void confirm_blankNo_throws() {
        CustomException ex = assertThrows(CustomException.class,
                () -> service.confirmRecharge("  ", 9L));
        assertEqualsMsg("充值单号不能为空", ex);
        verify(rechargeRecordMapper, never()).casConfirm(any(), any());
    }

    @Test
    void confirm_recordMissing_throws() {
        doReturn(null).when(service).getByRechargeNo("RCX");
        CustomException ex = assertThrows(CustomException.class,
                () -> service.confirmRecharge("RCX", 9L));
        assertEqualsMsg("充值单不存在", ex);
    }

    private RechargeRecord buildRecord() {
        RechargeRecord record = new RechargeRecord();
        record.setId(5L);
        record.setMemberId(10L);
        record.setAmount(new BigDecimal("100"));
        return record;
    }

    private void assertEqualsMsg(String expected, Exception ex) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, ex.getMessage());
    }
}
