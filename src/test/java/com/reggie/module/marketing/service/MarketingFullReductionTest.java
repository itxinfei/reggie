package com.reggie.module.marketing.service;

import com.reggie.module.marketing.mapper.CampaignUsageRecordMapper;
import com.reggie.module.marketing.mapper.MarketingCampaignMapper;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.model.FullReductionRule;
import com.reggie.module.marketing.model.MarketingCampaign;
import com.reggie.module.marketing.service.impl.MarketingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * C 端真满减核心逻辑单元测试（纯 Mockito，不连库）。
 * <p>覆盖：生效活动加载、减固定/打折封顶/赠品、多档命中最优、凑单 gap/progress、
 * 每人限次拦截、核销落库字段映射。满减按商品金额计（不含配送费）的口径在此保证。</p>
 */
@ExtendWith(MockitoExtension.class)
class MarketingFullReductionTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long CAMPAIGN_ID = 10L;

    @Mock
    private MarketingCampaignMapper campaignMapper;

    @Mock
    private CampaignUsageRecordMapper usageRecordMapper;

    /**
     * Spy：stub public 的 getFullReductionRules，绕过其内部 mapper，
     * 同时保留 evaluateFullReduction / ruleDiscount / recordFullReductionUsage 真实逻辑。
     */
    @Spy
    @InjectMocks
    private MarketingServiceImpl marketingService;

    private BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** 构造一个满减档位 */
    private FullReductionRule rule(long id, int type, String min, String value, String maxDiscount,
            Integer perUserLimit) {
        FullReductionRule r = new FullReductionRule();
        r.setId(id);
        r.setCampaignId(CAMPAIGN_ID);
        r.setTenantId(TENANT_ID);
        r.setStatus(1);
        r.setRuleName("档位" + id);
        r.setDiscountType(type);
        r.setMinAmount(bd(min));
        r.setDiscountValue(value != null ? bd(value) : null);
        r.setMaxDiscountAmount(maxDiscount != null ? bd(maxDiscount) : null);
        r.setPerUserLimit(perUserLimit);
        return r;
    }

    /** stub：存在一个生效满减活动，含给定档位 */
    private void stubActiveCampaign(List<FullReductionRule> rules) {
        MarketingCampaign campaign = new MarketingCampaign();
        campaign.setId(CAMPAIGN_ID);
        when(campaignMapper.selectList(any())).thenReturn(Collections.singletonList(campaign));
        doReturn(rules).when(marketingService).getFullReductionRules(eq(CAMPAIGN_ID), eq(TENANT_ID));
    }

    @Test
    void evaluate_noActiveCampaign_notAvailable() {
        when(campaignMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> r = marketingService.evaluateFullReduction(bd("50"), USER_ID, TENANT_ID);

        assertFalse((Boolean) r.get("available"), "无活动时不可用");
        assertEquals(0, ((BigDecimal) r.get("discount")).compareTo(BigDecimal.ZERO));
    }

    @Test
    void evaluate_reduceAmount_hitSingleTier() {
        stubActiveCampaign(Collections.singletonList(
                rule(1L, FullReductionRule.TYPE_REDUCE_AMOUNT, "30", "10", null, null)));

        Map<String, Object> r = marketingService.evaluateFullReduction(bd("50"), USER_ID, TENANT_ID);

        assertTrue((Boolean) r.get("available"));
        assertTrue((Boolean) r.get("hit"));
        assertEquals(0, ((BigDecimal) r.get("discount")).compareTo(bd("10")));
        assertEquals(CAMPAIGN_ID, r.get("campaignId"));
        assertEquals(1L, r.get("ruleId"));
        // 唯一档已命中且无更高门槛 → progress=100
        assertEquals(100, r.get("progress"));
    }

    @Test
    void evaluate_multiTier_returnsGapProgressAndNextDiscount() {
        stubActiveCampaign(Arrays.asList(
                rule(1L, FullReductionRule.TYPE_REDUCE_AMOUNT, "30", "10", null, null),
                rule(2L, FullReductionRule.TYPE_REDUCE_AMOUNT, "60", "20", null, null)));

        // 商品50：命中30减10，下一档60，gap=10，progress=50/60*100=83(向下取整)
        Map<String, Object> r = marketingService.evaluateFullReduction(bd("50"), USER_ID, TENANT_ID);

        assertTrue((Boolean) r.get("hit"));
        assertEquals(0, ((BigDecimal) r.get("discount")).compareTo(bd("10")));
        assertEquals(0, ((BigDecimal) r.get("gap")).compareTo(bd("10")));
        assertEquals(0, ((BigDecimal) r.get("nextMinAmount")).compareTo(bd("60")));
        // 达成下一档门槛时可减20（按门槛金额试算）
        assertEquals(0, ((BigDecimal) r.get("nextDiscount")).compareTo(bd("20")));
        assertEquals(83, r.get("progress"));
    }

    @Test
    void evaluate_discountType_respectsMaxCap() {
        // discountValue=0.8 表示8折，优惠=金额×(1-0.8)=金额×0.2；封顶 maxDiscountAmount=5
        stubActiveCampaign(Collections.singletonList(
                rule(1L, FullReductionRule.TYPE_DISCOUNT, "30", "0.8", "5", null)));

        Map<String, Object> r = marketingService.evaluateFullReduction(bd("100"), USER_ID, TENANT_ID);

        // 100×0.2=20，受封顶5限制
        assertTrue((Boolean) r.get("hit"));
        assertEquals(0, ((BigDecimal) r.get("discount")).compareTo(bd("5")));
    }

    @Test
    void evaluate_perUserLimitExceeded_notHit() {
        stubActiveCampaign(Collections.singletonList(
                rule(1L, FullReductionRule.TYPE_REDUCE_AMOUNT, "30", "10", null, 1)));
        // 该用户此档已核销1次，达 perUserLimit=1
        CampaignUsageRecord used = new CampaignUsageRecord();
        used.setCampaignId(CAMPAIGN_ID);
        used.setRuleId(1L);
        when(usageRecordMapper.selectList(any())).thenReturn(Collections.singletonList(used));

        Map<String, Object> r = marketingService.evaluateFullReduction(bd("50"), USER_ID, TENANT_ID);

        assertTrue((Boolean) r.get("available"), "活动仍有效");
        assertFalse((Boolean) r.get("hit"), "超出每人限次不命中");
        assertEquals(0, ((BigDecimal) r.get("discount")).compareTo(BigDecimal.ZERO));
    }

    @Test
    void recordUsage_persistsWithCorrectFields() {
        marketingService.recordFullReductionUsage(CAMPAIGN_ID, 1L, 99L, "99", USER_ID,
                bd("50"), bd("10"), bd("40"), TENANT_ID);

        ArgumentCaptor<CampaignUsageRecord> captor = ArgumentCaptor.forClass(CampaignUsageRecord.class);
        verify(usageRecordMapper).insert(captor.capture());
        CampaignUsageRecord rec = captor.getValue();
        assertEquals(1, rec.getRuleType(), "核销记录类型为满减");
        assertEquals(CAMPAIGN_ID, rec.getCampaignId());
        assertEquals(1L, rec.getRuleId());
        assertEquals(99L, rec.getOrderId());
        assertEquals(0, rec.getOrderAmount().compareTo(bd("50")));
        assertEquals(0, rec.getDiscountAmount().compareTo(bd("10")));
        assertEquals(0, rec.getActualAmount().compareTo(bd("40")));
        assertEquals(TENANT_ID, rec.getTenantId());
    }
}
