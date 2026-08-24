package com.reggie.module.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.order.model.Orders;
import com.reggie.module.user.model.User;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.dashboard.service.DashboardService;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 数据概览仪表盘服务实现
 * <p>
 * Redis数据流向说明：
 * 1. 前端请求 DashboardController → 本Service
 * 2. 优先查询Redis缓存（Hash / String / ZSet）
 * 3. 缓存命中：直接返回，延迟 < 5ms
 * 4. 缓存未命中：查询MySQL → 计算结果 → 回填Redis → 返回
 * 5. Redis不可用时：自动降级为直接查询MySQL
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DashboardServiceImpl implements DashboardService {

    /**
     * Redis Key前缀
     */
    private static final String KEY_OVERVIEW = "dashboard:overview:";
    private static final String KEY_TREND = "dashboard:trend:";
    private static final String KEY_ORDER_STATUS = "dashboard:order-status:";
    private static final String KEY_HOT_DISHES = "dashboard:hot-dishes:";

    /**
     * 缓存过期时间
     */
    private static final long TTL_OVERVIEW = 5;      // 概览：5分钟
    private static final long TTL_TREND = 30;          // 趋势：30分钟
    private static final long TTL_ORDER_STATUS = 2;    // 订单状态：2分钟
    private static final long TTL_HOT_DISHES = 15;     // 热销菜品：15分钟

    /** 订单服务 */
    /** 订单聚合 Mapper */
    @Autowired
    private OrderMapper orderMapper;

    /** 用户服务 */
    @Autowired
    private UserService userService;

    /** 员工服务 */
    @Autowired
    private EmployeeService employeeService;

    /**
     * RedisTemplate 设置为非必须注入，当Redis不可用时自动降级
     * 降级策略：直接查询MySQL，无缓存加速
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 概览数据 ====================

    /**
     * 获取今日概览数据（订单、营收、用户等）
     *
     * @param tenantId 租户ID
     * @return 概览数据Map
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Redis Hash返回Object类型，需要泛型转换
    public Map<String, Object> getOverview(Long tenantId) {
        // tenantId为null时不使用缓存（超级管理员视图，避免数据串租户）
        String cacheKey = tenantId != null ? KEY_OVERVIEW + tenantId : null;

        // [Redis] 尝试从缓存获取（仅当tenantId不为null时）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    log.debug("[Dashboard] Redis命中 - 概览数据 key={}", cacheKey);
                    Map<String, Object> result = new LinkedHashMap<>();
                    cached.forEach((k, v) -> result.put(String.valueOf(k), v));
                    // 修正缓存来源标记
                    result.put("cacheSource", "Redis");
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL", e);
            }
        }

        // [MySQL] 缓存未命中，查询数据库
        log.info("[Dashboard] Redis未命中，查询MySQL计算概览 key={}", cacheKey);
        Map<String, Object> overview;
        try {
            overview = computeOverview(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算概览异常", e);
            // 异常时不缓存，避免错误数据长时间生效
            return fallbackErrorResult("概览数据查询失败");
        }

        // [Redis] 回填缓存（仅缓存有效数据）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Map<String, Object> hashData = new HashMap<>(overview);
                redisTemplate.opsForHash().putAll(cacheKey, hashData);
                redisTemplate.expire(cacheKey, TTL_OVERVIEW, TimeUnit.MINUTES);
                log.info("[Dashboard] 概览数据已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败", e);
            }
        }

        return overview;
    }

    /**
     * 计算今日概览指标（直接查MySQL）
     */
    private Map<String, Object> computeOverview(Long tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long originalTenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            log.info("[Dashboard] tenantId为null，查询全量数据（超级管理员视图）");
        }
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 今日时间范围
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

            // 修改点：改用 SQL 聚合，避免 list 全量载入今日订单到内存（消除 OOM 风险）
            List<Map<String, Object>> orderStats = orderMapper.statOrderByStatus(todayStart, todayEnd);
            int totalOrders = 0;
            int pendingOrders = 0;
            int completedOrders = 0;
            int cancelledOrders = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Map<String, Object> row : orderStats) {
                int status = ((Number) row.get("status")).intValue();
                long cnt = ((Number) row.get("cnt")).longValue();
                BigDecimal amt = (BigDecimal) row.get("amt");
                totalOrders += cnt;
                if (status == Orders.STATUS_PENDING_PAY || status == Orders.STATUS_ORDERED) {
                    pendingOrders += cnt;
                } else if (status == Orders.STATUS_COMPLETED) {
                    completedOrders += cnt;
                } else if (status == Orders.STATUS_CANCELLED) {
                    cancelledOrders += cnt;
                }
                // 已取消/已退款订单不计入营业额
                if (status != Orders.STATUS_CANCELLED && status != Orders.STATUS_REFUNDED) {
                    totalRevenue = totalRevenue.add(amt != null ? amt : BigDecimal.ZERO);
                }
            }

            BigDecimal avgAmount = totalOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // User实体已添加createTime字段，按今日注册时间过滤
            LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
            userQw.between(User::getCreateTime, todayStart, todayEnd);
            int totalUsers = (int) userService.count(userQw);

            // 有效员工数
            LambdaQueryWrapper<Employee> empQw = new LambdaQueryWrapper<>();
            empQw.eq(Employee::getStatus, 1);
            int activeEmployees = (int) employeeService.count(empQw);

            BigDecimal completionRate = totalOrders > 0
                    ? BigDecimal.valueOf(completedOrders).divide(BigDecimal.valueOf(totalOrders), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.put("totalOrders", totalOrders);
            result.put("totalRevenue", totalRevenue.setScale(2, RoundingMode.HALF_UP));
            result.put("pendingOrders", pendingOrders);
            result.put("completedOrders", completedOrders);
            result.put("cancelledOrders", cancelledOrders);
            result.put("avgAmount", avgAmount);
            result.put("totalUsers", totalUsers);
            result.put("activeEmployees", activeEmployees);
            result.put("completionRate", completionRate);
            result.put("cacheSource", "MySQL");

            return result;
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
    }

    // ==================== 趋势数据 ====================

    /**
     * 获取最近7天订单趋势数据
     *
     * @param tenantId 租户ID
     * @return 趋势数据列表
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Redis缓存返回Object类型，需要泛型转换
    public List<Map<String, Object>> getTrend(Long tenantId) {
        // tenantId为null时不使用缓存（超级管理员视图，避免数据串租户）
        String cacheKey = tenantId != null ? KEY_TREND + tenantId : null;

        // [Redis] 尝试从缓存获取（仅当tenantId不为null时）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("[Dashboard] Redis命中 - 趋势数据 key={}", cacheKey);
                    List<Map<String, Object>> result = (List<Map<String, Object>>) cached;
                    result.forEach(m -> m.put("cacheSource", "Redis"));
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL", e);
            }
        }

        // [MySQL] 缓存未命中
        log.info("[Dashboard] Redis未命中，查询MySQL计算趋势 key={}", cacheKey);
        List<Map<String, Object>> trend;
        try {
            trend = computeTrend(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算趋势异常", e);
            // 异常时不缓存，避免错误数据长时间生效，下次请求可重新计算
            return new ArrayList<>();
        }

        // [Redis] 回填缓存（仅缓存有效数据，空结果不缓存）
        if (isRedisAvailable() && trend != null && !trend.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, trend, TTL_TREND, TimeUnit.MINUTES);
                log.info("[Dashboard] 趋势数据已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败", e);
            }
        }

        return trend;
    }

    /**
     * 计算最近7天趋势（一次查询，Java层按日期分组，减少DB压力）
     */
    private List<Map<String, Object>> computeTrend(Long tenantId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 修改点：改用 SQL 按日聚合，避免 list 全量载入近7天订单
            LocalDateTime rangeStart = LocalDate.now().minusDays(6).atStartOfDay();
            LocalDateTime rangeEnd   = LocalDate.now().atTime(LocalTime.MAX);
            List<Map<String, Object>> dayStats = orderMapper.statOrderByDay(rangeStart, rangeEnd, Orders.STATUS_COMPLETED);
            Map<LocalDate, Map<String, Object>> byDate = new LinkedHashMap<>();
            for (Map<String, Object> row : dayStats) {
                LocalDate day = ((Date) row.get("day")).toLocalDate();
                long cnt = ((Number) row.get("cnt")).longValue();
                BigDecimal amt = (BigDecimal) row.get("amt");
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("orderCount", (int) cnt);
                m.put("revenue", amt != null ? amt : BigDecimal.ZERO);
                byDate.put(day, m);
            }

            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                Map<String, Object> dayData = byDate.get(date);
                int count = dayData != null ? (int) dayData.get("orderCount") : 0;
                BigDecimal amount = dayData != null ? (BigDecimal) dayData.get("revenue") : BigDecimal.ZERO;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("date", date.format(fmt));
                item.put("orderCount", count);
                item.put("revenue", amount.setScale(2, RoundingMode.HALF_UP));
                item.put("cacheSource", "MySQL");
                trend.add(item);
            }
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return trend;
    }

    // ==================== 订单状态分布 ====================

    /**
     * 获取订单状态分布统计
     *
     * @param tenantId 租户ID
     * @return 订单状态分布Map
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Redis Hash返回Object类型，需要泛型转换
    public Map<String, Object> getOrderStatusDistribution(Long tenantId) {
        // tenantId为null时不使用缓存（超级管理员视图，避免数据串租户）
        String cacheKey = tenantId != null ? KEY_ORDER_STATUS + tenantId : null;

        // [Redis] 尝试从缓存获取（仅当tenantId不为null时）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    log.debug("[Dashboard] Redis命中 - 订单状态分布 key={}", cacheKey);
                    Map<String, Object> result = new LinkedHashMap<>();
                    cached.forEach((k, v) -> result.put(String.valueOf(k), v));
                    // 修正缓存来源标记
                    result.put("cacheSource", "Redis");
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL", e);
            }
        }

        // [MySQL] 缓存未命中
        log.info("[Dashboard] Redis未命中，查询MySQL计算订单状态 key={}", cacheKey);
        Map<String, Object> distribution;
        try {
            distribution = computeOrderStatusDistribution(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算订单状态分布异常", e);
            // 异常时不缓存，避免错误数据长时间生效
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put("待付款", 0);
            errorResult.put("待派送/处理", 0);
            errorResult.put("派送中", 0);
            errorResult.put("已完成", 0);
            errorResult.put("已取消", 0);
            errorResult.put("cacheSource", "Error");
            return errorResult;
        }

        // [Redis] 回填缓存（仅缓存有效数据）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Map<String, Object> hashData = new HashMap<>(distribution);
                redisTemplate.opsForHash().putAll(cacheKey, hashData);
                redisTemplate.expire(cacheKey, TTL_ORDER_STATUS, TimeUnit.MINUTES);
                log.info("[Dashboard] 订单状态分布已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败", e);
            }
        }

        return distribution;
    }

    /**
     * 计算当前（今日）订单状态分布
     */
    private Map<String, Object> computeOrderStatusDistribution(Long tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);

            // 修改点：改用 SQL 聚合，避免 list 全量载入今日订单到内存
            List<Map<String, Object>> orderStats = orderMapper.statOrderByStatus(todayStart, todayEnd);
            int pendingPay = 0, ordered = 0, delivering = 0, completed = 0, cancelled = 0;
            for (Map<String, Object> row : orderStats) {
                int status = ((Number) row.get("status")).intValue();
                long cnt = ((Number) row.get("cnt")).longValue();
                switch (status) {
                    case Orders.STATUS_PENDING_PAY: pendingPay += cnt; break;
                    case Orders.STATUS_ORDERED: ordered += cnt; break;
                    case Orders.STATUS_DELIVERING: delivering += cnt; break;
                    case Orders.STATUS_COMPLETED: completed += cnt; break;
                    case Orders.STATUS_CANCELLED: cancelled += cnt; break;
                    default: break;
                }
            }

            result.put("待付款", pendingPay);
            result.put("待派送/处理", ordered);
            result.put("派送中", delivering);
            result.put("已完成", completed);
            result.put("已取消", cancelled);
            result.put("cacheSource", "MySQL");
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
        return result;
    }

    // ==================== 热销菜品 ====================

    /**
     * 获取热销菜品排行榜
     *
     * @param tenantId 租户ID
     * @param limit 返回数量限制
     * @return 热销菜品列表
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"}) // Redis ZSet返回Object类型，需要泛型转换
    public List<Map<String, Object>> getHotDishes(Long tenantId, int limit) {
        // tenantId为null时不使用缓存（超级管理员视图，避免数据串租户）
        String cacheKey = tenantId != null ? KEY_HOT_DISHES + tenantId : null;

        // [Redis ZSet] 尝试从缓存获取（仅当tenantId不为null时）
        if (isRedisAvailable() && cacheKey != null) {
            try {
                Set<ZSetOperations.TypedTuple<Object>> topSet =
                        redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, 0, limit - 1);
                if (topSet != null && !topSet.isEmpty()) {
                    log.debug("[Dashboard] Redis命中 - 热销菜品 ZSet key={}", cacheKey);
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (ZSetOperations.TypedTuple<Object> tuple : topSet) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", String.valueOf(tuple.getValue()));
                        item.put("count", tuple.getScore() != null ? tuple.getScore().intValue() : 0);
                        result.add(item);
                    }
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis ZSet读取异常，降级查询MySQL", e);
            }
        }

        // [MySQL] 缓存未命中，计算热销菜品
        log.info("[Dashboard] Redis ZSet未命中，查询MySQL key={}", cacheKey);
        List<Map<String, Object>> hotDishes;
        try {
            hotDishes = computeHotDishes(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算热销菜品异常", e);
            hotDishes = new ArrayList<>();
        }

        // [Redis ZSet] 回填缓存（使用Pipeline保证原子性）
        if (isRedisAvailable() && !hotDishes.isEmpty()) {
            try {
                // 使用新Key写入，完成后rename，避免中间状态被读取
                String tempKey = cacheKey + ":temp";
                redisTemplate.delete(tempKey);

                // 修改点：final引用确保lambda中可用（hotDishes在catch中可能被重赋值）
                final List<Map<String, Object>> finalHotDishes = hotDishes;

                // 使用Pipeline批量写入，减少网络往返
                List<Object> results = redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                    for (Map<String, Object> dish : finalHotDishes) {
                        String name = String.valueOf(dish.get("name"));
                        Object countObj = dish.get("count");
                        double score = countObj instanceof Integer ? (Integer) countObj
                                : countObj instanceof Long ? ((Long) countObj).doubleValue() : 0;
                        connection.zAdd(tempKey.getBytes(), score, name.getBytes());
                    }
                    connection.expire(tempKey.getBytes(), java.util.concurrent.TimeUnit.MINUTES.toSeconds(TTL_HOT_DISHES));
                    return null;
                });

                // Pipeline成功后，使用RENAME原子替换（Redis RENAME命令会原子覆盖目标Key）
                // 修改点：移除多余的delete操作，RENAME本身已原子替换目标Key
                redisTemplate.rename(tempKey, cacheKey);

                log.info("[Dashboard] 热销菜品已缓存至Redis ZSet key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis ZSet回填失败", e);
            }
        }

        return hotDishes.size() <= limit ? hotDishes : hotDishes.subList(0, limit);
    }

    /**
     * 计算热销菜品排名（直接查MySQL - 今日订单明细聚合）
     * Redis ZSet数据结构：dashboard:hot-dishes:{tenantId}
     * Score = 商品销售数量（累计），Member = 商品名称
     */
    private List<Map<String, Object>> computeHotDishes(Long tenantId) {
        Long originalTenantId = BaseContext.getCurrentTenantId();
        try {
            BaseContext.setCurrentTenantId(tenantId);

            // 修改点：改用 SQL 聚合，避免 list 全量载入今日订单及明细到内存
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
            List<Map<String, Object>> dishStats = orderMapper.statHotDishes(start, end);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : dishStats) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", row.get("name"));
                item.put("count", ((Number) row.get("cnt")).intValue());
                item.put("cacheSource", "MySQL");
                result.add(item);
            }
            return result;
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
    }

    // ==================== 系统健康 ====================

    /**
     * 获取系统健康状态（Redis、数据库、JVM信息）
     *
     * @return 系统健康状态Map
     */
    @Override
    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();

        // Redis状态检查
        boolean redisOk = false;
        String redisInfo = "未连接";
        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForValue().set("dashboard:health-check", "ok", 10, TimeUnit.SECONDS);
                String checkVal = (String) redisTemplate.opsForValue().get("dashboard:health-check");
                redisOk = "ok".equals(checkVal);
                redisInfo = redisOk ? "正常" : "读写异常";
            } catch (Exception e) {
                log.warn("Dashboard Redis 健康检查异常：{}", e.getMessage(), e);
                redisInfo = "异常: " + e.getMessage();
            }
        }
        health.put("redisAvailable", redisOk);
        health.put("redisInfo", redisInfo);

        // 数据库状态：通过简单查询验证
        boolean dbOk = false;
        try {
            int userCount = (int) userService.count();
            dbOk = userCount >= 0;
            health.put("dbInfo", "正常 (" + userCount + " 用户)");
        } catch (Exception e) {
            log.warn("Dashboard 数据库健康检查异常：{}", e.getMessage(), e);
            health.put("dbInfo", "异常: " + e.getMessage());
        }
        health.put("dbAvailable", dbOk);

        // 系统信息
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("osName", System.getProperty("os.name"));
        health.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        health.put("totalMemoryMB", Runtime.getRuntime().totalMemory() / 1024 / 1024);
        health.put("freeMemoryMB", Runtime.getRuntime().freeMemory() / 1024 / 1024);

        return health;
    }

    /**
     * 检查Redis是否可用
     *
     * @return Redis是否可用
     */
    @Override
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    /**
     * 清除指定租户的 Dashboard 缓存（概览 + 订单状态分布 + 趋势 + 热销菜品）
     * 订单状态变更时调用，防止 Redis 缓存导致数据不实
     */
    @Override
    public void clearOverviewCache(Long tenantId) {
        if (!isRedisAvailable()) return;
        try {
            String overviewKey = KEY_OVERVIEW + tenantId;
            String statusKey = KEY_ORDER_STATUS + tenantId;
            String trendKey = KEY_TREND + tenantId;
            String hotDishesKey = KEY_HOT_DISHES + tenantId;
            redisTemplate.delete(overviewKey);
            redisTemplate.delete(statusKey);
            redisTemplate.delete(trendKey);
            redisTemplate.delete(hotDishesKey);
            log.info("[Dashboard] 已清除缓存 overviewKey={}, statusKey={}, trendKey={}, hotDishesKey={}",
                    overviewKey, statusKey, trendKey, hotDishesKey);
        } catch (Exception e) {
            log.warn("[Dashboard] 清除缓存失败", e);
        }
    }

    /**
     * 异常降级返回统一错误提示Map，防止异常穿透
     * @param errorMsg 错误提示信息
     * @return 包含错误提示的Map
     */
    private Map<String, Object> fallbackErrorResult(String errorMsg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", 0);
        result.put("totalRevenue", 0);
        result.put("pendingOrders", 0);
        result.put("completedOrders", 0);
        result.put("cancelledOrders", 0);
        result.put("avgAmount", 0);
        result.put("totalUsers", 0);
        result.put("activeEmployees", 0);
        result.put("completionRate", 0);
        result.put("cacheSource", "Error");
        result.put("errorMsg", errorMsg);
        return result;
    }
}








