package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 推荐反馈 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RecommendationFeedbackMapper extends BaseMapper<RecommendationFeedback> {

    /**
     * 统计指定天数内各反馈类型的数量
     *
     * @param startTime 起始时间
     * @param tenantId 租户ID（null表示不过滤）
     * @return 每行: feedback_type, cnt
     */
    @Select("<script>" +
            "SELECT feedback_type, COUNT(*) AS cnt FROM recommendation_feedback " +
            "WHERE create_time >= #{startTime} " +
            "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "GROUP BY feedback_type ORDER BY feedback_type" +
            "</script>")
    List<Map<String, Object>> countByTypeSince(@Param("startTime") String startTime,
                                                @Param("tenantId") Long tenantId);

    /**
     * 按推荐算法统计点击率(CTR)和转化率(CVR)
     *
     * @param startTime 起始时间
     * @param tenantId 租户ID（null表示不过滤）
     * @return 每行: algorithm, click_cnt, order_cnt, total_cnt
     */
    @Select("<script>" +
            "SELECT rc.algorithm, " +
            "  SUM(CASE WHEN rf.feedback_type = 1 THEN 1 ELSE 0 END) AS click_cnt, " +
            "  SUM(CASE WHEN rf.feedback_type = 4 THEN 1 ELSE 0 END) AS order_cnt, " +
            "  COUNT(*) AS total_cnt " +
            "FROM recommendation_feedback rf " +
            "INNER JOIN recommendation_cache rc ON rf.recommend_cache_id = rc.id " +
            "WHERE rf.create_time >= #{startTime} " +
            "<if test='tenantId != null'>AND rf.tenant_id = #{tenantId}</if>" +
            "GROUP BY rc.algorithm" +
            "</script>")
    List<Map<String, Object>> countByAlgorithmSince(@Param("startTime") String startTime,
                                                     @Param("tenantId") Long tenantId);
}
