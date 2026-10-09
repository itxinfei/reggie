package com.reggie.module.ai.service;

import com.reggie.module.dish.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.reggie.module.dish.mapper.DishMapper;

/**
 * AI 模块缓存服务
 *
 * <p>缓存策略：
 * <ul>
 *   <li>菜品数据缓存：避免每次请求都查询全量菜品列表（按租户隔离，防止跨租户串数据）</li>
 *   <li>供应商配置缓存：减少 DB 查询（manager 层已有本地缓存，此处做兜底）</li>
 *   <li>用户画像摘要缓存：减少重复计算</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
public class AICacheService {

    /**
     * 菜品列表缓存（dish → 格式化字符串）。
     * <p>修改点(2026-09-18)：改为按租户隔离。原实现为全局单份缓存，
     * 而 dishMapper 查询受 MyBatis-Plus 租户插件按当前线程 tenant_id 过滤，
     * 导致先访问的租户缓存被其他租户复用，推荐出错误门店的菜品。</p>
     */
    private final Map<String, DishCacheEntry> dishCacheByTenant = new ConcurrentHashMap<>();

    /** 菜品缓存有效期（5分钟） */
    private static final long DISH_CACHE_TTL = 5 * 60 * 1000L;

    @Resource
    private DishMapper dishMapper;

    /**
     * 获取格式化的菜品列表（带缓存，按当前租户隔离）
     * <p>缓存 5 分钟，期间菜品 CRUD 操作会导致缓存失效。
     */
    public String getFormattedDishList() {
        Long tenantId = resolveCurrentTenantId();
        DishCacheEntry entry = dishCacheByTenant.get(tenantKey(tenantId));
        long now = System.currentTimeMillis();
        if (entry != null && entry.cacheTime > 0 && (now - entry.cacheTime) < DISH_CACHE_TTL) {
            return entry.cachedList;
        }
        return rebuildDishCache(tenantId);
    }

    /**
     * 刷新当前租户的菜品缓存（在菜品 CRUD 后调用）
     */
    public void refreshDishCache() {
        Long tenantId = resolveCurrentTenantId();
        rebuildDishCache(tenantId);
        log.debug("菜品列表缓存已刷新: tenantId={}", tenantId);
    }

    // ==================== 内部方法 ====================

    private String rebuildDishCache(Long tenantId) {
        // 双重检查：可能在等待锁时已被其他线程重建
        DishCacheEntry existing = dishCacheByTenant.get(tenantKey(tenantId));
        long now = System.currentTimeMillis();
        if (existing != null && existing.cacheTime > 0 && (now - existing.cacheTime) < DISH_CACHE_TTL) {
            return existing.cachedList;
        }
        synchronized (this) {
            existing = dishCacheByTenant.get(tenantKey(tenantId));
            if (existing != null && existing.cacheTime > 0
                    && (System.currentTimeMillis() - existing.cacheTime) < DISH_CACHE_TTL) {
                return existing.cachedList;
            }
        }

        long start = System.currentTimeMillis();
        List<Dish> availableDishes = dishMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .eq(Dish::getIsDeleted, 0)
                        .orderByDesc(Dish::getSort)
        );

        StringBuilder sb = new StringBuilder();
        for (Dish dish : availableDishes) {
            sb.append(String.format("[%d] %s - ¥%.2f - %s\n",
                    dish.getId(), dish.getName(),
                    dish.getPrice() != null ? dish.getPrice().doubleValue() : 0,
                    dish.getDescription() != null ? dish.getDescription() : "暂无描述"));
        }

        DishCacheEntry entry = new DishCacheEntry(sb.toString(), now);
        dishCacheByTenant.put(tenantKey(tenantId), entry);
        log.debug("菜品列表缓存重建完成: tenantId={}, dishes={}, time={}ms",
                tenantId, availableDishes.size(), System.currentTimeMillis() - start);
        return entry.cachedList;
    }

    /** 解析当前租户ID；上下文缺失（如测试/非 Web 线程）时回退默认租户 */
    private Long resolveCurrentTenantId() {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 0L;
    }

    private String tenantKey(Long tenantId) {
        return String.valueOf(tenantId);
    }

    /** 租户级菜品缓存条目 */
    private static final class DishCacheEntry {
        final String cachedList;
        volatile long cacheTime;

        DishCacheEntry(String cachedList, long cacheTime) {
            this.cachedList = cachedList;
            this.cacheTime = cacheTime;
        }
    }
}
