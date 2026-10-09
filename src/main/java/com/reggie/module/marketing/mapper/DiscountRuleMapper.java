package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.DiscountRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 折扣规则 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface DiscountRuleMapper extends BaseMapper<DiscountRule> {
}
