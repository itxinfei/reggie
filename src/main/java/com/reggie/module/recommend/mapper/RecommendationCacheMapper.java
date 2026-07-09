package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.RecommendationCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

/**
 * 推荐结果缓存Mapper
 */
@Mapper
public interface RecommendationCacheMapper extends BaseMapper<RecommendationCache> {

    /**
     * 查找用户最新的有效推荐缓存
     */
    @Select("SELECT * FROM recommendation_cache " +
            "WHERE user_id = #{userId} AND recommend_type = #{recommendType} " +
            "AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1")
    RecommendationCache findValidCache(@Param("userId") Long userId, @Param("recommendType") Integer recommendType);

    /**
     * 清理过期缓存
     */
    @Delete("DELETE FROM recommendation_cache WHERE expire_time <= NOW()")
    int deleteExpired();
}
