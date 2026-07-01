package com.reggie.module.member;

import com.reggie.common.BaseContext;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = {"classpath:schema-member.sql"})
public class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberLevelService memberLevelService;

    @Autowired
    private PointsRecordService pointsRecordService;

    @Autowired
    private RechargeRecordService rechargeRecordService;

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Autowired
    private CouponUserService couponUserService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
        if (memberLevelService.count() == 0) {
            MemberLevel l1 = new MemberLevel();
            l1.setId(1L); l1.setTenantId(1L); l1.setName("普通会员"); l1.setMinPoints(0L); l1.setDiscount(new BigDecimal("1.00"));
            memberLevelService.save(l1);
            MemberLevel l2 = new MemberLevel();
            l2.setId(2L); l2.setTenantId(1L); l2.setName("银卡会员"); l2.setMinPoints(100L); l2.setDiscount(new BigDecimal("0.95"));
            memberLevelService.save(l2);
            MemberLevel l3 = new MemberLevel();
            l3.setId(3L); l3.setTenantId(1L); l3.setName("金卡会员"); l3.setMinPoints(500L); l3.setDiscount(new BigDecimal("0.90"));
            memberLevelService.save(l3);
        }
    }

    @Test
    void testRegisterByPhone() {
        Member member = memberService.registerByPhone("13800138001", "张三");
        assertNotNull(member.getId());
        assertEquals("13800138001", member.getPhone());
        assertEquals("张三", member.getName());
        assertEquals(Long.valueOf(0L), member.getPoints());
        assertEquals(0, member.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(Integer.valueOf(1), member.getStatus());
        assertNotNull(member.getLevelId());
        assertEquals("普通会员", memberLevelService.getById(member.getLevelId()).getName());
    }

    @Test
    void testRecharge() {
        Member member = memberService.registerByPhone("13800138002", "李四");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("100.00"), new BigDecimal("10.00"), "WECHAT");
        Member updated = memberService.getById(member.getId());
        assertEquals(new BigDecimal("110.00"), updated.getBalance());
    }

    @Test
    void testDeductBalanceSuccess() {
        Member member = memberService.registerByPhone("13800138003", "王五");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("100.00"), BigDecimal.ZERO, "WECHAT");
        boolean ok = memberService.deductBalance(member.getId(), new BigDecimal("30.00"));
        assertTrue(ok);
        Member updated = memberService.getById(member.getId());
        assertEquals(new BigDecimal("70.00"), updated.getBalance());
    }

    @Test
    void testDeductBalanceInsufficient() {
        Member member = memberService.registerByPhone("13800138004", "赵六");
        rechargeRecordService.recharge(member.getId(), new BigDecimal("50.00"), BigDecimal.ZERO, "WECHAT");
        boolean ok = memberService.deductBalance(member.getId(), new BigDecimal("100.00"));
        assertFalse(ok);
    }

    @Test
    void testAddPoints() {
        Member member = memberService.registerByPhone("13800138005", "测试积分");
        memberService.addPoints(member.getId(), 50, "CONSUME", 100L);
        Member updated = memberService.getById(member.getId());
        assertEquals(Long.valueOf(50L), updated.getPoints());

        List<PointsRecord> records = pointsRecordService.list();
        assertEquals(1, records.size());
        PointsRecord record = records.get(0);
        assertEquals(member.getId(), record.getMemberId());
        assertEquals("IN", record.getType());
        assertEquals(Integer.valueOf(50), record.getPoints());
        assertEquals("CONSUME", record.getBizType());
        assertEquals(Long.valueOf(100L), record.getBizId());
    }

    @Test
    void testAddPointsLevelUpgrade() {
        Member member = memberService.registerByPhone("13800138006", "测试升级");
        assertEquals("普通会员", memberLevelService.getById(member.getLevelId()).getName());

        memberService.addPoints(member.getId(), 500, "CONSUME", 200L);
        Member updated = memberService.getById(member.getId());
        assertEquals("金卡会员", memberLevelService.getById(updated.getLevelId()).getName());
    }

    @Test
    void testCalculateDiscount() {
        Member member = memberService.registerByPhone("13800138007", "测试折扣");
        memberService.addPoints(member.getId(), 500, "CONSUME", 300L);
        BigDecimal discounted = memberService.calculateDiscount(member.getId(), new BigDecimal("100.00"));
        assertEquals(new BigDecimal("90.00").setScale(2, RoundingMode.HALF_UP), discounted);
    }

    @Test
    void testClaimCoupon() {
        Member member = memberService.registerByPhone("13800138008", "测试领券");
        CouponTemplate template = new CouponTemplate();
        template.setTenantId(1L);
        template.setName("满100减20");
        template.setType("FULL_REDUCE");
        template.setConditionAmount(new BigDecimal("100.00"));
        template.setDiscountAmount(new BigDecimal("20.00"));
        template.setTotalCount(100);
        template.setRemainCount(10);
        template.setValidDays(30);
        template.setStatus(1);
        couponTemplateService.save(template);

        boolean ok = couponTemplateService.claimCoupon(member.getId(), template.getId());
        assertTrue(ok);

        CouponTemplate updated = couponTemplateService.getById(template.getId());
        assertEquals(Integer.valueOf(9), updated.getRemainCount());

        List<CouponUser> userCoupons = couponUserService.list();
        assertEquals(1, userCoupons.size());
        CouponUser cu = userCoupons.get(0);
        assertEquals(member.getId(), cu.getMemberId());
        assertEquals(template.getId(), cu.getTemplateId());
        assertEquals("UNUSED", cu.getStatus());
        assertNotNull(cu.getCode());
    }

    @Test
    void testUseCoupon() {
        Member member = memberService.registerByPhone("13800138009", "测试用券");
        CouponTemplate template = new CouponTemplate();
        template.setTenantId(1L);
        template.setName("满50减10");
        template.setType("FULL_REDUCE");
        template.setConditionAmount(new BigDecimal("50.00"));
        template.setDiscountAmount(new BigDecimal("10.00"));
        template.setTotalCount(50);
        template.setRemainCount(50);
        template.setStatus(1);
        couponTemplateService.save(template);

        couponTemplateService.claimCoupon(member.getId(), template.getId());
        List<CouponUser> userCoupons = couponUserService.list();
        CouponUser cu = userCoupons.get(0);

        boolean ok = couponTemplateService.useCoupon(cu.getId(), 999L);
        assertTrue(ok);

        CouponUser updated = couponUserService.getById(cu.getId());
        assertEquals("USED", updated.getStatus());
        assertEquals(Long.valueOf(999L), updated.getOrderId());
        assertNotNull(updated.getUsedTime());
    }
}
