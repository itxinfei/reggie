package com.reggie.module.member.service.impl;

import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.mapper.RechargeRecordMapper;
import com.reggie.module.member.model.RechargeRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RechargeRecordServiceImpl#handleRechargePaid 在线充值资金路径纯 Mockito 测试（P1-4）。
 * 覆盖：CAS 成功入账、SUCCESS 回调幂等、CANCELLED 支付迟到不入账、加余额失败仅告警不抛、
 * 渠道号缺失兜底内部交易号。
 *
 * @author reggie
 * @since 2026-09-29
 */
@ExtendWith(MockitoExtension.class)
class RechargePaymentPaidTest {

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    private RechargeRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new RechargeRecordServiceImpl());
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "rechargeRecordMapper", rechargeRecordMapper);
    }

    @Test
    void paid_casOk_addBalanceOk() {
        doReturn(buildRecord("PENDING")).when(service).getById(6101L);
        when(rechargeRecordMapper.casSuccessByPayment(6101L, "CH1")).thenReturn(1);
        when(memberMapper.addBalance(eq(7001L), eq(new BigDecimal("100")), any())).thenReturn(1);

        service.handleRechargePaid(6101L, "T1", "CH1");

        verify(memberMapper).addBalance(eq(7001L), eq(new BigDecimal("100")), any());
    }

    @Test
    void paid_cas0_alreadySuccess_idempotent() {
        doReturn(buildRecord("SUCCESS")).when(service).getById(6101L);
        when(rechargeRecordMapper.casSuccessByPayment(6101L, "CH1")).thenReturn(0);

        service.handleRechargePaid(6101L, "T1", "CH1");

        verify(memberMapper, never()).addBalance(any(), any(), any());
    }

    @Test
    void paid_cas0_cancelled_latePayment_notAdded() {
        doReturn(buildRecord("CANCELLED")).when(service).getById(6101L);
        when(rechargeRecordMapper.casSuccessByPayment(6101L, "CH1")).thenReturn(0);

        service.handleRechargePaid(6101L, "T1", "CH1");

        // 超时取消后支付迟到：钱已收不能入账，只告警，不得再加余额
        verify(memberMapper, never()).addBalance(any(), any(), any());
    }

    @Test
    void paid_casOk_addBalanceFail0_doesNotThrow() {
        doReturn(buildRecord("PENDING")).when(service).getById(6101L);
        when(rechargeRecordMapper.casSuccessByPayment(6101L, "CH1")).thenReturn(1);
        when(memberMapper.addBalance(eq(7001L), eq(new BigDecimal("100")), any())).thenReturn(0);

        // 终态已落、加余额失败：仅严重告警人工补录，不向回调抛异常
        assertDoesNotThrow(() -> service.handleRechargePaid(6101L, "T1", "CH1"));
        verify(memberMapper).addBalance(eq(7001L), eq(new BigDecimal("100")), any());
    }

    @Test
    void paid_channelTradeNoBlank_fallbackPaymentTradeNo() {
        doReturn(buildRecord("PENDING")).when(service).getById(6101L);
        when(rechargeRecordMapper.casSuccessByPayment(6101L, "T1")).thenReturn(1);
        when(memberMapper.addBalance(eq(7001L), eq(new BigDecimal("100")), any())).thenReturn(1);

        service.handleRechargePaid(6101L, "T1", "");

        // 渠道号缺失（mock）兜底存内部交易号
        verify(rechargeRecordMapper).casSuccessByPayment(6101L, "T1");
    }

    private RechargeRecord buildRecord(String status) {
        RechargeRecord record = new RechargeRecord();
        record.setId(6101L);
        record.setMemberId(7001L);
        record.setAmount(new BigDecimal("100"));
        record.setStatus(status);
        return record;
    }
}
