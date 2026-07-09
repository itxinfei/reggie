package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.Employee;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.entity.User;
import com.reggie.service.DashboardService;
import com.reggie.service.EmployeeService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.stream.Collectors;

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

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmployeeService employeeService;

    /**
     * RedisTemplate 设置为非必须注入，当Redis不可用时自动降级
     * 降级策略：直接查询MySQL，无缓存加速
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 概览数据 ====================

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Object> getOverview(Long tenantId) {
        String cacheKey = KEY_OVERVIEW + tenantId;

        // [Redis] 尝试从缓存获取
        if (isRedisAvailable()) {
            try {
                Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    log.info("[Dashboard] Redis命中 - 概览数据 key={}", cacheKey);
                    Map<String, Object> result = new LinkedHashMap<>();
                    cached.forEach((k, v) -> result.put(String.valueOf(k), v));
                    // 修正缓存来源标记
                    result.put("cacheSource", "Redis");
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL: {}", e.getMessage());
            }
        }

        // [MySQL] 缓存未命中，查询数据库
        log.info("[Dashboard] Redis未命中，查询MySQL计算概览 key={}", cacheKey);
        Map<String, Object> overview;
        try {
            overview = computeOverview(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算概览异常: {}", e.getMessage(), e);
            overview = fallbackErrorResult("概览数据查询失败");
        }

        // [Redis] 回填缓存
        if (isRedisAvailable()) {
            try {
                Map<String, Object> hashData = new HashMap<>(overview);
                redisTemplate.opsForHash().putAll(cacheKey, hashData);
                redisTemplate.expire(cacheKey, TTL_OVERVIEW, TimeUnit.MINUTES);
                log.info("[Dashboard] 概览数据已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败: {}", e.getMessage());
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

            // 查询今日订单
            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.between(Orders::getOrderTime, todayStart, todayEnd);
            List<Orders> todayOrders = orderService.list(qw);

            int totalOrders = todayOrders.size();
            int pendingOrders = 0;
            int completedOrders = 0;
            int cancelledOrders = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Orders o : todayOrders) {
                BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(amt);
                if (o.getStatus() != null) {
                    if (o.getStatus() == Orders.STATUS_PENDING_PAY || o.getStatus() == Orders.STATUS_ORDERED) {
                        pendingOrders++;
                    } else if (o.getStatus() == Orders.STATUS_COMPLETED) {
                        completedOrders++;
                    } else if (o.getStatus() == Orders.STATUS_CANCELLED) {
                        cancelledOrders++;
                    }
                }
            }

            BigDecimal avgAmount = totalOrders > 0
                    ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 修改点：User实体已添加createTime字段，按今日注册时间过滤
            LambdaQueryWrapper<User> userQw = new LambdaQueryWrapper<>();
            userQw.between(User::getCreateTime, todayStart, todayEnd);
            int totalUsers = userService.count(userQw);

            // 有效员工数
            LambdaQueryWrapper<Employee> empQw = new LambdaQueryWrapper<>();
            empQw.eq(Employee::getStatus, 1);
            int activeEmployees = employeeService.count(empQw);

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

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> getTrend(Long tenantId) {
        String cacheKey = KEY_TREND + tenantId;

        // [Redis] 尝试从缓存获取
        if (isRedisAvailable()) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.info("[Dashboard] Redis命中 - 趋势数据 key={}", cacheKey);
                    List<Map<String, Object>> result = (List<Map<String, Object>>) cached;
                    result.forEach(m -> m.put("cacheSource", "Redis"));
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL: {}", e.getMessage());
            }
        }

        // [MySQL] 缓存未命中
        log.info("[Dashboard] Redis未命中，查询MySQL计算趋势 key={}", cacheKey);
        List<Map<String, Object>> trend;
        try {
            trend = computeTrend(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算趋势异常: {}", e.getMessage(), e);
            trend = new ArrayList<>();
        }

        // [Redis] 回填缓存
        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, trend, TTL_TREND, TimeUnit.MINUTES);
                log.info("[Dashboard] 趋势数据已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败: {}", e.getMessage());
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

            // 一次性查出近7天所有订单
            LocalDateTime rangeStart = LocalDate.now().minusDays(6).atStartOfDay();
            LocalDateTime rangeEnd   = LocalDate.now().atTime(LocalTime.MAX);
            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.between(Orders::getOrderTime, rangeStart, rangeEnd);
            List<Orders> allOrders = orderService.list(qw);

            // 按日期分组聚合
            Map<LocalDate, List<Orders>> byDate = allOrders.stream()
                    .filter(o -> o.getOrderTime() != null)
                    .collect(Collectors.groupingBy(o -> o.getOrderTime().toLocalDate()));

            for (int i = 6; i >= 0; i--) {
                LocalDate date = LocalDate.now().minusDays(i);
                List<Orders> dayOrders = byDate.getOrDefault(date, new ArrayList<>());
                int count = dayOrders.size();
                // 营业额只统计已完成订单
                BigDecimal amount = dayOrders.stream()
                        .filter(o -> o.getStatus() != null && o.getStatus() == Orders.STATUS_COMPLETED)
                        .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<String, Object> getOrderStatusDistribution(Long tenantId) {
        String cacheKey = KEY_ORDER_STATUS + tenantId;

        // [Redis] 尝试从缓存获取
        if (isRedisAvailable()) {
            try {
                Map<Object, Object> cached = redisTemplate.opsForHash().entries(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    log.info("[Dashboard] Redis命中 - 订单状态分布 key={}", cacheKey);
                    Map<String, Object> result = new LinkedHashMap<>();
                    cached.forEach((k, v) -> result.put(String.valueOf(k), v));
                    // 修正缓存来源标记
                    result.put("cacheSource", "Redis");
                    return result;
                }
            } catch (Exception e) {
                log.warn("[Dashboard] Redis读取异常，降级查询MySQL: {}", e.getMessage());
            }
        }

        // [MySQL] 缓存未命中
        log.info("[Dashboard] Redis未命中，查询MySQL计算订单状态 key={}", cacheKey);
        Map<String, Object> distribution;
        try {
            distribution = computeOrderStatusDistribution(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算订单状态分布异常: {}", e.getMessage(), e);
            distribution = new LinkedHashMap<>();
            distribution.put("待付款", 0);
            distribution.put("待派送/处理", 0);
            distribution.put("派送中", 0);
            distribution.put("已完成", 0);
            distribution.put("已取消", 0);
            distribution.put("cacheSource", "Error");
        }

        // [Redis] 回填缓存
        if (isRedisAvailable()) {
            try {
                Map<String, Object> hashData = new HashMap<>(distribution);
                redisTemplate.opsForHash().putAll(cacheKey, hashData);
                redisTemplate.expire(cacheKey, TTL_ORDER_STATUS, TimeUnit.MINUTES);
                log.info("[Dashboard] 订单状态分布已缓存至Redis key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis回填失败: {}", e.getMessage());
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

            LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
            qw.between(Orders::getOrderTime, todayStart, todayEnd);
            List<Orders> orders = orderService.list(qw);

            int pendingPay = 0, ordered = 0, delivering = 0, completed = 0, cancelled = 0;

            for (Orders o : orders) {
                if (o.getStatus() == null) continue;
                switch (o.getStatus()) {
                    case Orders.STATUS_PENDING_PAY: pendingPay++; break;
                    case Orders.STATUS_ORDERED: ordered++; break;
                    case Orders.STATUS_DELIVERING: delivering++; break;
                    case Orders.STATUS_COMPLETED: completed++; break;
                    case Orders.STATUS_CANCELLED: cancelled++; break;
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

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> getHotDishes(Long tenantId, int limit) {
        String cacheKey = KEY_HOT_DISHES + tenantId;

        // [Redis ZSet] 尝试从缓存获取
        if (isRedisAvailable()) {
            try {
                Set<ZSetOperations.TypedTuple<Object>> topSet =
                        redisTemplate.opsForZSet().reverseRangeWithScores(cacheKey, 0, limit - 1);
                if (topSet != null && !topSet.isEmpty()) {
                    log.info("[Dashboard] Redis命中 - 热销菜品 ZSet key={}", cacheKey);
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
                log.warn("[Dashboard] Redis ZSet读取异常，降级查询MySQL: {}", e.getMessage());
            }
        }

        // [MySQL] 缓存未命中，计算热销菜品
        log.info("[Dashboard] Redis ZSet未命中，查询MySQL key={}", cacheKey);
        List<Map<String, Object>> hotDishes;
        try {
            hotDishes = computeHotDishes(tenantId);
        } catch (Exception e) {
            log.error("[Dashboard] 计算热销菜品异常: {}", e.getMessage(), e);
            hotDishes = new ArrayList<>();
        }

        // [Redis ZSet] 回填缓存
        if (isRedisAvailable() && !hotDishes.isEmpty()) {
            try {
                redisTemplate.delete(cacheKey);
                for (Map<String, Object> dish : hotDishes) {
                    String name = String.valueOf(dish.get("name"));
                    Object countObj = dish.get("count");
                    double score = countObj instanceof Integer ? (Integer) countObj
                            : countObj instanceof Long ? ((Long) countObj).doubleValue() : 0;
                    redisTemplate.opsForZSet().add(cacheKey, name, score);
                }
                redisTemplate.expire(cacheKey, TTL_HOT_DISHES, TimeUnit.MINUTES);
                log.info("[Dashboard] 热销菜品已缓存至Redis ZSet key={}", cacheKey);
            } catch (Exception e) {
                log.warn("[Dashboard] Redis ZSet回填失败: {}", e.getMessage());
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

            // 获取今日所有订单ID
            LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
            orderQw.between(Orders::getOrderTime,
                    LocalDate.now().atStartOfDay(),
                    LocalDate.now().atTime(LocalTime.MAX));
            orderQw.select(Orders::getId);
            List<Orders> todayOrders = orderService.list(orderQw);

            if (todayOrders.isEmpty()) {
                return new ArrayList<>();
            }

            // 获取订单明细
            List<Long> orderIds = todayOrders.stream()
                    .map(Orders::getId)
                    .collect(Collectors.toList());
            // 修改点：防御性检查，避免空列表传入IN子句导致SQL语法错误
            if (orderIds.isEmpty()) {
                return new ArrayList<>();
            }
            LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
            detailQw.in(OrderDetail::getOrderId, orderIds);
            List<OrderDetail> details = orderDetailService.list(detailQw);

            // 按商品名称聚合数量
            Map<String, Integer> dishCount = new LinkedHashMap<>();
            for (OrderDetail d : details) {
                if (d.getName() != null) {
                    dishCount.merge(d.getName(),
                            d.getNumber() != null ? d.getNumber() : 0,
                            Integer::sum);
                }
            }

            // 按销量降序排列
            return dishCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .map(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", e.getKey());
                        item.put("count", e.getValue());
                        item.put("cacheSource", "MySQL");
                        return item;
                    })
                    .collect(Collectors.toList());
        } finally {
            BaseContext.setCurrentTenantId(originalTenantId);
        }
    }

    // ==================== 系统健康 ====================

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
                redisInfo = "异常: " + e.getMessage();
            }
        }
        health.put("redisAvailable", redisOk);
        health.put("redisInfo", redisInfo);

        // 数据库状态：通过简单查询验证
        boolean dbOk = false;
        try {
            int userCount = userService.count();
            dbOk = userCount >= 0;
            health.put("dbInfo", "正常 (" + userCount + " 用户)");
        } catch (Exception e) {
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

    @Override
    public boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    /**
     * 修改点：异常降级返回统一错误提示Map，防止异常穿透
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
