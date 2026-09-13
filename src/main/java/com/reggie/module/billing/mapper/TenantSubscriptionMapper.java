package com.reggie.module.billing.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.module.billing.model.TenantSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 租户订阅 Mapper。租户自身查询由多租户插件自动追加 tenant_id；
 * 平台运营跨租户查询使用 {@link #adminPage} 并以 @InterceptorIgnore 显式绕过、条件自控。
 *
 * @author reggie
 * @since 2026-09-12
 */
@Mapper
public interface TenantSubscriptionMapper extends BaseMapper<TenantSubscription> {

    /**
     * 平台运营跨租户分页查询订阅记录（绕过租户拦截器，过滤条件显式传入）。
     *
     * @param page     分页对象
     * @param status   状态（可选）
     * @param tenantId 租户ID（可选）
     * @return 分页结果
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
            + "SELECT * FROM tenant_subscription WHERE is_deleted = 0"
            + "<if test='status != null'> AND status = #{status}</if>"
            + "<if test='tenantId != null'> AND tenant_id = #{tenantId}</if>"
            + " ORDER BY id DESC"
            + "</script>")
    IPage<TenantSubscription> adminPage(IPage<TenantSubscription> page,
                                        @Param("status") Integer status,
                                        @Param("tenantId") Long tenantId);

    /**
     * 批量把已过到期时间但仍"生效中"的订阅置为"已过期"。
     * 定时任务无租户上下文，必须绕过租户拦截器（否则 fail-closed 追加 tenant_id=-1 导致一条都更新不到）。
     *
     * @param now           当前时间
     * @param activeStatus  生效中状态值
     * @param expiredStatus 已过期状态值
     * @return 受影响条数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE tenant_subscription SET status = #{expiredStatus}, update_time = #{now} "
            + "WHERE is_deleted = 0 AND status = #{activeStatus} AND end_time < #{now}")
    int expireOverdueBatch(@Param("now") LocalDateTime now,
                           @Param("activeStatus") int activeStatus,
                           @Param("expiredStatus") int expiredStatus);
}
