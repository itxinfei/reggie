package com.reggie.module.marketing.service;

import com.reggie.module.marketing.mapper.CampaignUsageRecordMapper;
import com.reggie.module.marketing.mapper.FlashSaleMapper;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.service.impl.MarketingToolServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定「缺口2：秒杀库存与限购记录只增不减」的修复。
 *
 * <p>审查结论曾断言 {@code FlashSaleMapper} 全仓只有 {@code deductStock}(+) 没有回退、
 * {@code campaign_usage_record} 的 delete 零命中。当前 HEAD 已实现 {@code revertStock}(-)
 * 与 {@code deleteUsageRecord}(delete)，并在 {@code releaseFlashSaleUsage} 中于取消/退款链路统一调用。
 * 本测试不连库，断言：释放秒杀占用时<b>确实回退库存并删除限购记录</b>，且保持幂等守卫。</p>
 */
@ExtendWith(MockitoExtension.class)
class FlashSaleUsageReleaseTest {

    @Mock
    private CampaignUsageRecordMapper campaignUsageRecordMapper;

    @Mock
    private FlashSaleMapper flashSaleMapper;

    @Spy
    @InjectMocks
    private MarketingToolServiceImpl service;

    private CampaignUsageRecord flashRec(Long campaignId, Integer qty, Long userId) {
        CampaignUsageRecord r = new CampaignUsageRecord();
        r.setCampaignId(campaignId);
        r.setRuleType(3); // 3 = 秒杀
        r.setQuantity(qty);
        r.setUserId(userId);
        return r;
    }

    @Test
    void release_flashSaleOrder_invokesRevertAndDelete() {
        CampaignUsageRecord r = flashRec(5L, 2, 7L);
        when(campaignUsageRecordMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        service.releaseFlashSaleUsage(100L, 1L);

        // 修复点：回退秒杀名额（sold_quantity - qty）
        verify(flashSaleMapper).revertStock(eq(5L), eq(2));
        // 修复点：释放限购额度（DELETE campaign_usage_record）
        verify(flashSaleMapper).deleteUsageRecord(eq(5L), eq(7L), eq(1L));
    }

    @Test
    void release_noUsageRecords_noRevertNorDelete() {
        when(campaignUsageRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.releaseFlashSaleUsage(100L, 1L);

        verify(flashSaleMapper, never()).revertStock(anyLong(), anyInt());
        verify(flashSaleMapper, never()).deleteUsageRecord(anyLong(), anyLong(), anyLong());
    }

    @Test
    void release_nullTenantId_revertsStockButSkipsDelete() {
        CampaignUsageRecord r = flashRec(5L, 2, 7L);
        when(campaignUsageRecordMapper.selectList(any())).thenReturn(Collections.singletonList(r));

        service.releaseFlashSaleUsage(100L, null);

        verify(flashSaleMapper).revertStock(eq(5L), eq(2));
        // tenantId 为 null 时删除不做归属过滤，按契约不调用
        verify(flashSaleMapper, never()).deleteUsageRecord(anyLong(), anyLong(), anyLong());
    }

    @Test
    void release_nullOrderId_returnsImmediately() {
        service.releaseFlashSaleUsage(null, 1L);

        verify(campaignUsageRecordMapper, never()).selectList(any());
    }
}
