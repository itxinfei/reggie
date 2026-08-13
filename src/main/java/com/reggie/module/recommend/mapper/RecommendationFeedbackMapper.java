package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
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
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 租户过滤已由本 SQL 的 {@code <if test='tenantId != null'>} 分支显式控制
     * （tenantId 为 null 表示不过滤，由调用方传入 {@code BaseContext.getCurrentTenantId()}），
     * 若再叠加租户插件自动注入 {@code tenant_id = ?}，会造成重复过滤，
     * 且在无租户上下文时误注入 {@code tenant_id = -1}，导致"null 不过滤"语义失效。
     *
     * @param startTime 起始时间
     * @param tenantId 租户ID（null表示不过滤）
     * @return 每行: feedback_type, cnt
     */
    @InterceptorIgnore(tenantLine = "true")
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
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 租户过滤已由本 SQL 的 {@code <if test='tenantId != null'>} 分支显式控制
     * （tenantId 为 null 表示不过滤，由调用方传入 {@code BaseContext.getCurrentTenantId()}），
     * 若再叠加租户插件自动注入 {@code tenant_id = ?}，会造成重复过滤，
     * 且在无租户上下文时误注入 {@code tenant_id = -1}，导致"null 不过滤"语义失效。
     *
     * @param startTime 起始时间
     * @param tenantId 租户ID（null表示不过滤）
     * @return 每行: algo_name, click_cnt, order_cnt, total_cnt
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT rc.algo_name, " +
            "  SUM(CASE WHEN rf.feedback_type = 1 THEN 1 ELSE 0 END) AS click_cnt, " +
            "  SUM(CASE WHEN rf.feedback_type = 4 THEN 1 ELSE 0 END) AS order_cnt, " +
            "  COUNT(*) AS total_cnt " +
            "FROM recommendation_feedback rf " +
            "INNER JOIN recommendation_cache rc ON rf.recommend_cache_id = rc.id " +
            "WHERE rf.create_time >= #{startTime} " +
            "<if test='tenantId != null'>AND rf.tenant_id = #{tenantId}</if>" +
            "GROUP BY rc.algo_name" +
            "</script>")
    List<Map<String, Object>> countByAlgorithmSince(@Param("startTime") String startTime,
                                                     @Param("tenantId") Long tenantId);
}
