package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.reggie.module.recommend.model.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户浏览历史 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    /**
     * 查询用户最近N条浏览记录
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 本方法服务 C 端用户接口（RecommendController.getRecentHistory），
     * C 端 session 无 tenantId 上下文，租户插件会注入 tenant_id = -1 导致返回空集。
     * 隔离依赖 {@code user_id} 参数：C 端会话 userId 已校验登录态，用户 ID 空间全局唯一，
     * 无需再叠租户条件（跨店浏览数据在推荐场景下有意允许）。
     *
     * @param userId 用户ID
     * @param limit 条数
     * @return 浏览记录列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM user_browse_history WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<BrowseHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计用户浏览最多的菜品类别 TOP N
     * <p>
     * 同 findRecentByUserId：C 端接口依赖 userId 隔离，跳过租户插件避免 C 端 session 返回空集。
     *
     * @param userId 用户ID
     * @param limit 条数
     * @return 最多浏览的菜品列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT bh.target_id, bh.target_name, COUNT(*) as view_count " +
            "FROM user_browse_history bh " +
            "WHERE bh.user_id = #{userId} AND bh.target_type = 1 AND bh.is_deleted = 0 " +
            "GROUP BY bh.target_id, bh.target_name " +
            "ORDER BY view_count DESC LIMIT #{limit}")
    List<Map<String, Object>> findTopViewedDishes(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定用户在某时间范围内的浏览记录数
     * <p>
     * 同 findRecentByUserId：C 端 session 跳过租户插件。
     *
     * @param userId 用户ID
     * @param startTime 起始时间
     * @return 浏览记录数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM user_browse_history " +
            "WHERE user_id = #{userId} AND is_deleted = 0 AND create_time >= #{startTime}")
    int countByUserSince(@Param("userId") Long userId, @Param("startTime") String startTime);

    /**
     * 批量统计多个用户在某时间范围内的浏览记录数
     * 用于批量推送时一次性获取所有用户的浏览活跃度，避免 N+1 查询
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 调用方（MarketingCampaignServiceImpl）传入的 userIds 已由上游 selectList 按 tenantId 过滤，
     * 若再叠租户插件会因上下文租户与调用方租户不一致导致漏数。
     *
     * @param userIds 用户ID列表
     * @param startTime 起始时间（字符串格式，如 "2024-01-01 00:00:00"）
     * @return 每行: user_id, browse_count
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT user_id, COUNT(*) as browse_count " +
            "FROM user_browse_history " +
            "WHERE is_deleted = 0 AND user_id IN " +
            "<foreach collection='userIds' item='uid' open='(' separator=',' close=')'>" +
            "#{uid}" +
            "</foreach> " +
            "AND create_time >= #{startTime} " +
            "GROUP BY user_id" +
            "</script>")
    List<Map<String, Object>> countByUsersSince(@Param("userIds") List<Long> userIds,
                                                 @Param("startTime") String startTime);

    /**
     * 统计每日浏览行为趋势（按日期分组）
     * 用于概览页浏览趋势折线图
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 租户过滤已由本 SQL 的 {@code <if test='tenantId != null'>} 分支显式控制
     * （tenantId 为 null 表示不过滤，由调用方传入 {@code BaseContext.getCurrentTenantId()}），
     * 若再叠加租户插件自动注入 {@code tenant_id = ?}，会造成重复过滤，
     * 且在无租户上下文时误注入 {@code tenant_id = -1}，导致"null 不过滤"语义失效。
     *
     * @param startTime 起始时间
     * @param tenantId  租户ID（null不过滤）
     * @return 每行: date(日期), browse_count(浏览数), cart_count(加购数)
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>" +
            "SELECT " +
            "  DATE(create_time) AS date, " +
            "  SUM(CASE WHEN action_type = 1 THEN 1 ELSE 0 END) AS browse_count, " +
            "  SUM(CASE WHEN action_type = 3 THEN 1 ELSE 0 END) AS cart_count " +
            "FROM user_browse_history " +
            "WHERE create_time >= #{startTime} " +
            "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date ASC" +
            "</script>")
    List<Map<String, Object>> countDailyTrend(@Param("startTime") String startTime,
                                               @Param("tenantId") Long tenantId);
}
