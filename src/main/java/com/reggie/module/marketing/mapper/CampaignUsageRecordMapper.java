package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 营销活动使用记录 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface CampaignUsageRecordMapper extends BaseMapper<CampaignUsageRecord> {
}
