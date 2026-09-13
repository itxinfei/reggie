package com.reggie.module.billing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.billing.model.BillingPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * SaaS套餐方案 Mapper（套餐为平台级全局表，已在多租户插件白名单中，不追加 tenant_id）
 *
 * @author reggie
 * @since 2026-09-12
 */
@Mapper
public interface BillingPlanMapper extends BaseMapper<BillingPlan> {
}
