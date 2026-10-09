package com.reggie.module.member.service.impl;

import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.PointsRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouponTemplateServiceImpl#exchangeWithPoints 积分兑换纯 Mockito 测试（P1-5）。
 * 覆盖：未开会员、券下架/不支持积分兑换、积分不足、成功兑换、发券失败等分支。
 *
 * @author reggie
 * @since 2026-09-29
 */
@ExtendWith(MockitoExtension.class)
class PointsExchangeServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PointsRecordService pointsRecordService;

    private CouponTemplateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = Mockito.spy(new CouponTemplateServiceImpl());
        ReflectionTestUtils.setField(service, "memberService", memberService);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "pointsRecordService", pointsRecordService);
    }

    @Test
    void memberNull_throws() {
        when(memberService.getByUserId(9001L)).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.exchangeWithPoints(9001L, 8001L));
        assertEquals("尚未开通会员，请先注册会员", ex.getMessage());
        verify(memberMapper, never()).deductPointsIfEnough(anyLong(), anyInt());
    }

    @Test
    void templatePointsPriceNull_throws() {
        when(memberService.getByUserId(9001L)).thenReturn(member());
        doReturn(template(null, 1)).when(service).getById(8001L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.exchangeWithPoints(9001L, 8001L));
        assertEquals("该券不支持积分兑换", ex.getMessage());
        verify(memberMapper, never()).deductPointsIfEnough(anyLong(), anyInt());
    }

    @Test
    void templateDisabled_throws() {
        when(memberService.getByUserId(9001L)).thenReturn(member());
        doReturn(template(100, 0)).when(service).getById(8001L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.exchangeWithPoints(9001L, 8001L));
        assertEquals("优惠券不存在或已下架", ex.getMessage());
    }

    @Test
    void notEnoughPoints_throws() {
        when(memberService.getByUserId(9001L)).thenReturn(member());
        doReturn(template(100, 1)).when(service).getById(8001L);
        when(memberMapper.deductPointsIfEnough(7001L, 100)).thenReturn(0);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.exchangeWithPoints(9001L, 8001L));
        assertEquals("积分不足", ex.getMessage());
        verify(pointsRecordService, never()).save(any(PointsRecord.class));
        verify(service, never()).claimCoupon(anyLong(), anyLong());
    }

    @Test
    void success_writesRecordAndClaims() {
        when(memberService.getByUserId(9001L)).thenReturn(member());
        doReturn(template(100, 1)).when(service).getById(8001L);
        when(memberMapper.deductPointsIfEnough(7001L, 100)).thenReturn(1);
        doReturn(true).when(service).claimCoupon(7001L, 8001L);

        service.exchangeWithPoints(9001L, 8001L);

        verify(pointsRecordService).save(any(PointsRecord.class));
        verify(service).claimCoupon(7001L, 8001L);
    }

    @Test
    void claimFails_throws() {
        when(memberService.getByUserId(9001L)).thenReturn(member());
        doReturn(template(100, 1)).when(service).getById(8001L);
        when(memberMapper.deductPointsIfEnough(7001L, 100)).thenReturn(1);
        doReturn(false).when(service).claimCoupon(7001L, 8001L);

        assertThrows(CustomException.class, () -> service.exchangeWithPoints(9001L, 8001L));
        verify(pointsRecordService).save(any(PointsRecord.class));
    }

    private Member member() {
        Member m = new Member();
        m.setId(7001L);
        m.setUserId(9001L);
        return m;
    }

    private CouponTemplate template(Integer pointsPrice, int status) {
        CouponTemplate t = new CouponTemplate();
        t.setId(8001L);
        t.setPointsPrice(pointsPrice);
        t.setStatus(status);
        return t;
    }
}
