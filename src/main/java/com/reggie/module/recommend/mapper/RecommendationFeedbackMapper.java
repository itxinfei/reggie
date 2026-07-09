package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐反馈Mapper
 */
@Mapper
public interface RecommendationFeedbackMapper extends BaseMapper<RecommendationFeedback> {
}
