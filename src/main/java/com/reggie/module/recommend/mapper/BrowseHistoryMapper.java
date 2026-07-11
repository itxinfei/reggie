package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.BrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户浏览历史 Mapper
 * 修改点：新增每日浏览/加购趋势统计查询，替换原先Math.random()假数据
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    /**
     * 查询用户最近N条浏览记录
     */
    @Select("SELECT * FROM user_browse_history WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<BrowseHistory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 统计用户浏览最多的菜品类别 TOP N
     */
    @Select("SELECT bh.target_id, bh.target_name, COUNT(*) as view_count " +
            "FROM user_browse_history bh " +
            "WHERE bh.user_id = #{userId} AND bh.target_type = 1 " +
            "GROUP BY bh.target_id, bh.target_name " +
            "ORDER BY view_count DESC LIMIT #{limit}")
    List<Map<String, Object>> findTopViewedDishes(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询指定用户在某时间范围内的浏览记录数
     */
    @Select("SELECT COUNT(*) FROM user_browse_history " +
            "WHERE user_id = #{userId} AND create_time >= #{startTime}")
    int countByUserSince(@Param("userId") Long userId, @Param("startTime") String startTime);

    /**
     * 统计每日浏览行为趋势（按日期分组）
     * 用于概览页浏览趋势折线图
     *
     * @param startTime 起始时间
     * @return 每行: date(日期), browse_count(浏览数), cart_count(加购数)
     */
    @Select("SELECT " +
            "  DATE(create_time) AS date, " +
            "  SUM(CASE WHEN action_type = 1 THEN 1 ELSE 0 END) AS browse_count, " +
            "  SUM(CASE WHEN action_type = 3 THEN 1 ELSE 0 END) AS cart_count " +
            "FROM user_browse_history " +
            "WHERE create_time >= #{startTime} " +
            "GROUP BY DATE(create_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> countDailyTrend(@Param("startTime") String startTime);
}
