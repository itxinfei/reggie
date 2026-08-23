package com.reggie.module.ai.service;

import com.reggie.module.dish.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import com.reggie.module.dish.mapper.DishMapper;

/**
 * AI 模块缓存服务
 *
 * <p>缓存策略：
 * <ul>
 *   <li>菜品数据缓存：避免每次请求都查询全量菜品列表</li>
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

    /** 菜品列表缓存（dish → 格式化字符串） */
    private volatile String cachedDishList;

    /** 菜品列表缓存时间戳 */
    private volatile long dishCacheTime = 0L;

    /** 菜品缓存有效期（5分钟） */
    private static final long DISH_CACHE_TTL = 5 * 60 * 1000L;

    @Resource
    private DishMapper dishMapper;

    /**
     * 获取格式化的菜品列表（带缓存）
     * <p>缓存 5 分钟，期间菜品 CRUD 操作会导致缓存失效。
     */
    public String getFormattedDishList() {
        long now = System.currentTimeMillis();
        if (cachedDishList != null && (now - dishCacheTime) < DISH_CACHE_TTL) {
            return cachedDishList;
        }
        return rebuildDishCache();
    }

    /**
     * 刷新菜品缓存（在菜品 CRUD 后调用）
     */
    public void refreshDishCache() {
        rebuildDishCache();
        log.debug("菜品列表缓存已刷新");
    }

    // ==================== 内部方法 ====================

    private synchronized String rebuildDishCache() {
        // 双重检查：可能在等待锁时已被其他线程刷新
        long now = System.currentTimeMillis();
        if (cachedDishList != null && (now - dishCacheTime) < DISH_CACHE_TTL) {
            return cachedDishList;
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

        this.cachedDishList = sb.toString();
        this.dishCacheTime = now;
        log.debug("菜品列表缓存重建完成: dishes={}, time={}ms", availableDishes.size(), System.currentTimeMillis() - start);
        return cachedDishList;
    }
}


