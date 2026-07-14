package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

/**
 * <p>
 * 推荐结果缓存 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RecommendationCacheMapper extends BaseMapper<RecommendationCache> {

    /**
     * 查找用户最新的有效推荐缓存
     *
     * @param userId 用户ID
     * @param recommendType 推荐类型
     * @param tenantId 租户ID（null表示不过滤）
     * @return 有效推荐缓存
     */
    @Select("<script>" +
            "SELECT * FROM recommendation_cache " +
            "WHERE user_id = #{userId} AND recommend_type = #{recommendType} " +
            "AND expire_time > NOW() " +
            "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if>" +
            "ORDER BY create_time DESC LIMIT 1" +
            "</script>")
    RecommendationCache findValidCache(@Param("userId") Long userId,
                                       @Param("recommendType") Integer recommendType,
                                       @Param("tenantId") Long tenantId);

    /**
     * 清理过期缓存
     *
     * @return 删除的过期缓存数量
     */
    @Delete("DELETE FROM recommendation_cache WHERE expire_time <= NOW()")
    int deleteExpired();
}
