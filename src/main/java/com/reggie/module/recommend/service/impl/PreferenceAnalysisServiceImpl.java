package com.reggie.module.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.*;
import com.reggie.module.recommend.mapper.BrowseHistoryMapper;
import com.reggie.module.recommend.mapper.UserPreferenceMapper;
import com.reggie.module.recommend.model.UserPreferenceTag;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户偏好分析服务实现
 * 基于订单历史和浏览记录，自动分析并更新用户口味、品类、价格、时段偏好标签
 */
@Slf4j
@Service
public class PreferenceAnalysisServiceImpl implements PreferenceAnalysisService {

    @Autowired
    private UserPreferenceMapper userPreferenceMapper;
    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishFlavorService dishFlavorService;

    @Override
    @Transactional
    public boolean analyzeUserPreferences(Long userId) {
        if (userId == null) return false;
        Long tenantId = BaseContext.getCurrentTenantId();

        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);

            // 1. 分析品类偏好
            analyzeCategoryPreference(userId, tenantId, thirtyDaysAgo);

            // 2. 分析口味偏好
            analyzeTastePreference(userId, tenantId, thirtyDaysAgo);

            // 3. 分析价格偏好
            String pricePref = analyzePricePreference(userId);
            updateOrCreateTag(userId, UserPreferenceTag.TAG_TYPE_PRICE, pricePref,
                    BigDecimal.ONE, UserPreferenceTag.SOURCE_ORDER);

            // 4. 分析时段偏好
            String timePref = analyzeTimePreference(userId);
            if (timePref != null) {
                updateOrCreateTag(userId, UserPreferenceTag.TAG_TYPE_TIME, timePref,
                        BigDecimal.ONE, UserPreferenceTag.SOURCE_ORDER);
            }

            log.info("[偏好分析] 用户{}偏好分析完成", userId);
            return true;
        } catch (Exception e) {
            log.error("[偏好分析] 用户{}偏好分析失败: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    // ==================== 品类偏好分析 ====================
    private void analyzeCategoryPreference(Long userId, Long tenantId, LocalDateTime since) {
        // 统计用户下单最多的菜品品类
        Map<Long, Integer> categoryCount = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();

        List<Orders> orders = getUserRecentOrders(userId, since);
        for (Orders order : orders) {
            List<OrderDetail> details = getOrderDetails(order.getId());
            for (OrderDetail detail : details) {
                Dish dish = dishService.getById(detail.getDishId());
                if (dish != null && dish.getCategoryId() != null) {
                    categoryCount.merge(dish.getCategoryId(), 1, Integer::sum);
                    if (!categoryNameMap.containsKey(dish.getCategoryId())) {
                        Category cat = categoryService.getById(dish.getCategoryId());
                        if (cat != null) {
                            categoryNameMap.put(dish.getCategoryId(), cat.getName());
                        }
                    }
                }
            }
        }

        // 清除旧品类标签
        clearTagsByType(userId, UserPreferenceTag.TAG_TYPE_CATEGORY);

        // 排序并记录TOP5品类偏好
        int totalOrders = categoryCount.values().stream().mapToInt(Integer::intValue).sum();
        categoryCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    String name = categoryNameMap.getOrDefault(e.getKey(), "其他");
                    double weight = totalOrders > 0 ? (double) e.getValue() / totalOrders : 0.2;
                    updateOrCreateTag(userId, UserPreferenceTag.TAG_TYPE_CATEGORY, name,
                            BigDecimal.valueOf(Math.min(weight, 1.0)), UserPreferenceTag.SOURCE_ORDER);
                });
    }

    // ==================== 口味偏好分析 ====================
    private void analyzeTastePreference(Long userId, Long tenantId, LocalDateTime since) {
        Map<String, Integer> flavorCount = new HashMap<>();

        List<Orders> orders = getUserRecentOrders(userId, since);
        for (Orders order : orders) {
            List<OrderDetail> details = getOrderDetails(order.getId());
            for (OrderDetail detail : details) {
                Dish dish = dishService.getById(detail.getDishId());
                if (dish != null) {
                    // 通过菜品口味表获取口味信息
                    LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
                    flavorWrapper.eq(DishFlavor::getDishId, dish.getId());
                    List<DishFlavor> flavors = dishFlavorService.list(flavorWrapper);
                    for (DishFlavor flavor : flavors) {
                        if (flavor.getValue() != null && !flavor.getValue().isEmpty()) {
                            // 提取口味关键词
                            String tasteKey = extractTasteKeyword(flavor.getValue());
                            if (tasteKey != null) {
                                flavorCount.merge(tasteKey, 1, Integer::sum);
                            }
                        }
                    }

                    // 从菜品名称和描述提取口味
                    if (dish.getName() != null) {
                        countTasteFromText(dish.getName(), flavorCount);
                    }
                    if (dish.getDescription() != null) {
                        countTasteFromText(dish.getDescription(), flavorCount);
                    }
                }
            }
        }

        // 清除旧口味标签
        clearTagsByType(userId, UserPreferenceTag.TAG_TYPE_TASTE);

        // 记录TOP5口味偏好
        int totalFlavors = flavorCount.values().stream().mapToInt(Integer::intValue).sum();
        flavorCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> {
                    double weight = totalFlavors > 0 ? (double) e.getValue() / totalFlavors : 0.2;
                    updateOrCreateTag(userId, UserPreferenceTag.TAG_TYPE_TASTE, e.getKey(),
                            BigDecimal.valueOf(Math.min(weight, 1.0)), UserPreferenceTag.SOURCE_ORDER);
                });
    }

    /**
     * 从文本中提取口味关键词
     */
    private String extractTasteKeyword(String text) {
        if (text == null) return null;
        String[] tasteKeywords = {
            "辣", "麻辣", "香辣", "酸辣", "微辣", "不辣", "清淡",
            "甜", "咸", "酸", "苦", "鲜",
            "蒜蓉", "葱油", "红烧", "清蒸", "水煮", "油炸", "烧烤",
            "咖喱", "孜然", "藤椒", "泡椒", "黑椒"
        };
        for (String keyword : tasteKeywords) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        return null;
    }

    private void countTasteFromText(String text, Map<String, Integer> flavorCount) {
        String taste = extractTasteKeyword(text);
        if (taste != null) {
            flavorCount.merge(taste, 1, Integer::sum);
        }
    }

    // ==================== 价格偏好分析 ====================

    @Override
    public String analyzePricePreference(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<Orders> orders = getUserRecentOrders(userId, thirtyDaysAgo);

        if (orders.isEmpty()) return "实惠型";

        // 计算平均客单价
        double avgAmount = orders.stream()
                .mapToDouble(o -> o.getAmount() != null ? o.getAmount().doubleValue() : 0)
                .average()
                .orElse(0);

        if (avgAmount < 20) return "经济型";
        if (avgAmount < 40) return "实惠型";
        if (avgAmount < 80) return "中档";
        return "高端";
    }

    // ==================== 时段偏好分析 ====================

    @Override
    public String analyzeTimePreference(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<Orders> orders = getUserRecentOrders(userId, thirtyDaysAgo);

        if (orders.isEmpty()) return null;

        Map<String, Integer> timeSlotCount = new LinkedHashMap<>();
        timeSlotCount.put("早餐时段", 0);
        timeSlotCount.put("午餐时段", 0);
        timeSlotCount.put("下午茶时段", 0);
        timeSlotCount.put("晚餐时段", 0);
        timeSlotCount.put("夜宵时段", 0);

        for (Orders order : orders) {
            if (order.getOrderTime() != null) {
                int hour = order.getOrderTime().getHour();
                if (hour >= 6 && hour < 10) timeSlotCount.merge("早餐时段", 1, Integer::sum);
                else if (hour >= 10 && hour < 14) timeSlotCount.merge("午餐时段", 1, Integer::sum);
                else if (hour >= 14 && hour < 17) timeSlotCount.merge("下午茶时段", 1, Integer::sum);
                else if (hour >= 17 && hour < 21) timeSlotCount.merge("晚餐时段", 1, Integer::sum);
                else timeSlotCount.merge("夜宵时段", 1, Integer::sum);
            }
        }

        return timeSlotCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ==================== 用户画像判断 ====================

    @Override
    public boolean isChurnWarningUser(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        // 30天内无订单
        List<Orders> recentOrders = getUserRecentOrders(userId, thirtyDaysAgo);
        if (!recentOrders.isEmpty()) return false;

        // 7天内有浏览记录
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        int browseCount = browseHistoryMapper.countByUserSince(userId, sevenDaysAgo.toString());
        return browseCount > 0;
    }

    @Override
    public boolean isHighFrequencyUser(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        List<Orders> recentOrders = getUserRecentOrders(userId, thirtyDaysAgo);
        return recentOrders.size() >= 8;
    }

    // ==================== 工具方法 ====================

    private List<Orders> getUserRecentOrders(Long userId, LocalDateTime since) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId)
               .ge(Orders::getOrderTime, since);
        return orderService.list(wrapper);
    }

    private List<OrderDetail> getOrderDetails(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        return orderDetailService.list(wrapper);
    }

    private void updateOrCreateTag(Long userId, Integer tagType, String tagName,
                                    BigDecimal tagValue, String source) {
        LambdaQueryWrapper<UserPreferenceTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferenceTag::getUserId, userId)
               .eq(UserPreferenceTag::getTagType, tagType)
               .eq(UserPreferenceTag::getTagName, tagName);

        UserPreferenceTag existing = userPreferenceMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setTagValue(tagValue);
            existing.setSource(source);
            userPreferenceMapper.updateById(existing);
        } else {
            UserPreferenceTag tag = new UserPreferenceTag();
            tag.setUserId(userId);
            tag.setTagType(tagType);
            tag.setTagName(tagName);
            tag.setTagValue(tagValue);
            tag.setSource(source);
            userPreferenceMapper.insert(tag);
        }
    }

    private void clearTagsByType(Long userId, Integer tagType) {
        LambdaQueryWrapper<UserPreferenceTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPreferenceTag::getUserId, userId)
               .eq(UserPreferenceTag::getTagType, tagType);
        userPreferenceMapper.delete(wrapper);
    }
}
