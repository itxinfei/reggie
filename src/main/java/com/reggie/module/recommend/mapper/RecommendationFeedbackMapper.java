package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐反馈 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface RecommendationFeedbackMapper extends BaseMapper<RecommendationFeedback> {
}
