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
     *
     * @param userId 用户ID
     * @param limit 条数
     * @return 浏览记录列表
     */
    @Select("SELECT * FROM user_browse_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<BrowseHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计用户浏览最多的菜品类别 TOP N
     *
     * @param userId 用户ID
     * @param limit 条数
     * @return 最多浏览的菜品列表
     */
    @Select("SELECT bh.target_id, bh.target_name, COUNT(*) as view_count " +
            "FROM user_browse_history bh " +
            "WHERE bh.user_id = #{userId} AND bh.target_type = 1 " +
            "GROUP BY bh.target_id, bh.target_name " +
            "ORDER BY view_count DESC LIMIT #{limit}")
    List<Map<String, Object>> findTopViewedDishes(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定用户在某时间范围内的浏览记录数
     *
     * @param userId 用户ID
     * @param startTime 起始时间
     * @return 浏览记录数
     */
    @Select("SELECT COUNT(*) FROM user_browse_history " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime}")
    int countByUserSince(@Param("userId") Long userId, @Param("startTime") String startTime);

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
