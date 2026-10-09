package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
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
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 租户过滤已由本 SQL 的 {@code <if test='tenantId != null'>} 分支显式控制
     * （tenantId 为 null 表示不过滤，由调用方传入 {@code BaseContext.getCurrentTenantId()}），
     * 若再叠加租户插件自动注入 {@code tenant_id = ?}，会造成重复过滤，
     * 且在无租户上下文时误注入 {@code tenant_id = -1}，导致"null 不过滤"语义失效。
     *
     * @param userId 用户ID
     * @param recommendType 推荐类型
     * @param tenantId 租户ID（null表示不过滤）
     * @return 有效推荐缓存
     */
    @InterceptorIgnore(tenantLine = "true")
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
     * <p>
     * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 跳过租户拦截器：
     * 该方法在应用启动（@PostConstruct，无租户上下文）时执行，若走租户插件会注入
     * {@code tenant_id = -1}，导致只清理系统级缓存、租户缓存永不失效；
     * 过期缓存清理是全局操作，与租户无关，须全局执行。
     *
     * @return 删除的过期缓存数量
     */
    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM recommendation_cache WHERE expire_time <= NOW()")
    int deleteExpired();
}
