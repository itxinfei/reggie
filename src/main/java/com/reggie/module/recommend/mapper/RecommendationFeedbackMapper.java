package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 推荐反馈 Mapper
 * 修改点：新增真实统计查询方法，替换原先的Math.random()假数据
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface RecommendationFeedbackMapper extends BaseMapper<RecommendationFeedback> {

    /**
     * 统计指定天数内各反馈类型的数量
     * 用于概览页反馈分布柱状图
     *
     * @param startTime 起始时间
     * @return 每行: feedback_type, cnt
     */
    @Select("SELECT feedback_type, COUNT(*) AS cnt FROM recommendation_feedback " +
            "WHERE create_time >= #{startTime} " +
            "GROUP BY feedback_type ORDER BY feedback_type")
    List<Map<String, Object>> countByTypeSince(@Param("startTime") String startTime);

    /**
     * 按推荐算法统计点击率(CTR)和转化率(CVR)
     * 用于算法效果对比柱状图
     * 关联 recommendation_cache 获取算法类型
     *
     * @param startTime 起始时间
     * @return 每行: algorithm, click_cnt, order_cnt, total_cnt
     */
    @Select("SELECT rc.algorithm, " +
            "  SUM(CASE WHEN rf.feedback_type = 1 THEN 1 ELSE 0 END) AS click_cnt, " +
            "  SUM(CASE WHEN rf.feedback_type = 4 THEN 1 ELSE 0 END) AS order_cnt, " +
            "  COUNT(*) AS total_cnt " +
            "FROM recommendation_feedback rf " +
            "INNER JOIN recommendation_cache rc ON rf.recommend_cache_id = rc.id " +
            "WHERE rf.create_time >= #{startTime} " +
            "GROUP BY rc.algorithm")
    List<Map<String, Object>> countByAlgorithmSince(@Param("startTime") String startTime);
}
