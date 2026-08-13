package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.mapper.UserProfileMapper;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.model.UserProfile;
import com.reggie.module.ai.service.UserProfileService;
import com.reggie.module.recommend.mapper.UserPreferenceMapper;
import com.reggie.module.recommend.model.UserPreferenceTag;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI用户画像服务实现
 * 聚合多源数据构建用户长期记忆
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class UserProfileServiceImpl extends ServiceImpl<UserProfileMapper, UserProfile> implements UserProfileService {

    /** 用户画像Mapper */
    @Resource
    private UserProfileMapper userProfileMapper;

    /** 用户偏好分析服务 */
    @Resource
    private PreferenceAnalysisService preferenceAnalysisService;

    /** 用户偏好标签Mapper */
    @Resource
    private UserPreferenceMapper userPreferenceMapper;

    /** 菜品Mapper */
    @Resource
    private DishMapper dishMapper;

    /** 订单Mapper */
    @Resource
    private OrderMapper orderMapper;

    /** 订单明细Mapper */
    @Resource
    private OrderDetailMapper orderDetailMapper;

    /** AI消息记录Mapper */
    @Resource
    private AIMessageRecordMapper aiMessageRecordMapper;

    // ==================== 画像获取 ====================

    @Override
    public UserProfile getOrCreateProfile(Long userId) {
        if (userId == null) return null;
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setConfidence(new BigDecimal("0.1"));
            profile.setTotalConversations(0);
            profile.setTotalFeedbacks(0);
            // 首次创建时立即分析一次
            refreshProfileFields(profile);
            try {
                userProfileMapper.insert(profile);
            } catch (DuplicateKeyException e) {
                log.warn("并发创建用户画像冲突，重新查询已创建的画像 userId={}", userId);
                profile = userProfileMapper.selectByUserId(userId);
            }
        }
        return profile;
    }

    // ==================== 画像摘要构建 ====================

    @Override
    public String buildProfileSummary(Long userId) {
        if (userId == null) return "";

        UserProfile profile = getOrCreateProfile(userId);
        if (profile == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【用户画像】\n");

        // 口味偏好
        if (profile.getTasteTags() != null && !profile.getTasteTags().isEmpty()) {
            sb.append("口味偏好：").append(profile.getTasteTags()).append("\n");
        }
        // 品类偏好
        if (profile.getCategoryTags() != null && !profile.getCategoryTags().isEmpty()) {
            sb.append("喜欢品类：").append(profile.getCategoryTags()).append("\n");
        }
        // 忌口
        if (profile.getDislikedTags() != null && !profile.getDislikedTags().isEmpty()) {
            sb.append("忌口/不喜欢：").append(profile.getDislikedTags()).append("\n");
        }
        // 价格偏好
        if (profile.getPricePreference() != null) {
            sb.append("价格偏好：").append(translatePricePref(profile.getPricePreference())).append("\n");
        }
        // 常点菜品
        if (profile.getFrequentDishIds() != null && !profile.getFrequentDishIds().isEmpty()) {
            sb.append("常点菜品：").append(profile.getFrequentDishIds()).append("\n");
        }
        // 就餐方式偏好
        if (profile.getPreferredDiningType() != null) {
            sb.append("偏好就餐方式：").append(translateDiningType(profile.getPreferredDiningType())).append("\n");
        }
        // 时段偏好
        if (profile.getPreferredTimeSlot() != null) {
            sb.append("常点餐时段：").append(translateTimeSlot(profile.getPreferredTimeSlot())).append("\n");
        }
        // 历史反馈
        appendFeedbackSummary(userId, sb);

        sb.append("【推荐规则】\n")
          .append("1. 优先推荐符合用户口味和价格偏好的菜品\n")
          .append("2. 避免推荐用户忌口的菜品\n")
          .append("3. 推荐理由要结合用户历史偏好说明\n")
          .append("4. 如果用户历史反馈某类菜品不好，不要再推荐同类\n");

        return sb.toString();
    }

    // ==================== 画像刷新 ====================

    /** 画像刷新冷却时间（10分钟），避免每次对话都全量刷新 */
    // 修改点：新增节流机制，减少不必要的画像刷新计算
    private static final long REFRESH_COOLDOWN_MS = 10 * 60 * 1000;

    @Override
    public void refreshProfile(Long userId) {
        if (userId == null) return;
        UserProfile profile = getOrCreateProfile(userId);
        if (profile == null) return;

        refreshProfileFields(profile);

        // 更新分析时间和统计
        profile.setLastAnalyzedTime(java.time.LocalDateTime.now());
        profile.setTotalConversations(countUserConversations(userId));
        profile.setTotalFeedbacks(countUserFeedbacks(userId));

        // 计算置信度
        BigDecimal confidence = calculateConfidence(profile);
        profile.setConfidence(confidence);

        userProfileMapper.updateById(profile);
        log.info("用户画像已更新: userId={}, confidence={}", userId, confidence);
    }

    /**
     * 按需刷新用户画像（带节流）
     * 修改点：如果距上次刷新不足10分钟，跳过本次刷新
     *
     * @param userId 用户ID
     */
    @Override
    public void refreshIfNeeded(Long userId) {
        if (userId == null) return;
        UserProfile profile = getOrCreateProfile(userId);
        if (profile == null) return;
        if (profile.getLastAnalyzedTime() != null
                && System.currentTimeMillis() - profile.getLastAnalyzedTime().atZone(
                        java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() < REFRESH_COOLDOWN_MS) {
            log.debug("用户画像刷新跳过（冷却期内）: userId={}", userId);
            return;
        }
        refreshProfile(userId);
    }

    /**
     * 刷新画像字段（从各数据源重新分析）
     */
    private void refreshProfileFields(UserProfile profile) {
        Long userId = profile.getUserId();

        // 1. 从推荐模块获取已有偏好
        try {
            String pricePref = preferenceAnalysisService.analyzePricePreference(userId);
            profile.setPricePreference(pricePref);
        } catch (Exception e) {
            log.warn("获取价格偏好失败: userId={}", userId, e);
        }

        // 2. 从历史订单分析常点菜品
        try {
            List<Long> frequentDishIds = analyzeFrequentDishIds(userId);
            profile.setFrequentDishIds(frequentDishIds.stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        } catch (Exception e) {
            log.warn("分析常点菜品失败: userId={}", userId, e);
        }

        // 3. 从 AI 对话反馈中分析口味偏好
        try {
            analyzeTasteFromFeedback(profile);
        } catch (Exception e) {
            log.warn("分析口味偏好失败: userId={}", userId, e);
        }

        // 4. 从订单分析就餐偏好
        try {
            profile.setPreferredDiningType(analyzePreferredDiningType(userId));
            profile.setPreferredTimeSlot(analyzePreferredTimeSlot(userId));
        } catch (Exception e) {
            log.warn("分析就餐偏好失败: userId={}", userId, e);
        }
    }

    // ==================== 数据分析方法 ====================

    /**
     * 从历史订单分析常点菜品
     */
    private List<Long> analyzeFrequentDishIds(Long userId) {
        // 查询最近30天的订单
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getUserId, userId)
                .eq(Orders::getStatus, Orders.STATUS_COMPLETED)
                .ge(Orders::getOrderTime, java.time.LocalDateTime.now().minusDays(30))
                .last("LIMIT 50");
        List<Orders> recentOrders = orderMapper.selectList(orderWrapper);

        if (recentOrders.isEmpty()) return Collections.emptyList();

        // 统计菜品出现频率
        Map<Long, Integer> dishCount = new HashMap<>();
        for (Orders order : recentOrders) {
            // 使用 MP 查询订单明细
            List<OrderDetail> details = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>()
                    .eq(OrderDetail::getOrderId, order.getId())
            );
            if (details != null) {
                for (OrderDetail detail : details) {
                    if (detail.getDishId() != null) {
                        dishCount.merge(detail.getDishId(), 1, Integer::sum);
                    }
                }
            }
        }

        return dishCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 从 AI 反馈分析口味偏好
     */
    private void analyzeTasteFromFeedback(UserProfile profile) {
        // 查询用户最近的 AI 反馈记录
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getUserId, profile.getUserId())
                .isNotNull(AIMessageRecord::getFeedback)
                .orderByDesc(AIMessageRecord::getCreateTime)
                .last("LIMIT 100");
        List<AIMessageRecord> feedbacks = aiMessageRecordMapper.selectList(wrapper);

        if (feedbacks.isEmpty()) return;

        // 统计正向反馈中包含的口味关键词
        Set<String> goodTastes = new HashSet<>();
        Set<String> badTastes = new HashSet<>();
        String[] tasteKeywords = {"辣", "清淡", "甜", "酸", "麻", "鲜", "清淡", "重口", "素食", "海鲜"};

        for (AIMessageRecord feedback : feedbacks) {
            if (feedback.getContent() != null) {
                if ("good".equals(feedback.getFeedback())) {
                    for (String keyword : tasteKeywords) {
                        if (feedback.getContent().contains(keyword)) {
                            goodTastes.add(keyword);
                        }
                    }
                } else if ("bad".equals(feedback.getFeedback())) {
                    for (String keyword : tasteKeywords) {
                        if (feedback.getContent().contains(keyword)) {
                            badTastes.add(keyword);
                        }
                    }
                }
            }
        }

        if (!goodTastes.isEmpty()) {
            profile.setTasteTags(String.join(",", goodTastes));
        }
        if (!badTastes.isEmpty()) {
            profile.setDislikedTags(String.join(",", badTastes));
        }

        // 同时从推荐模块获取品类偏好
        try {
            List<UserPreferenceTag> categoryPrefs = userPreferenceMapper.selectList(
                    new LambdaQueryWrapper<UserPreferenceTag>()
                            .eq(UserPreferenceTag::getUserId, profile.getUserId())
                            .eq(UserPreferenceTag::getTagType, UserPreferenceTag.TAG_TYPE_CATEGORY)
                            .orderByDesc(UserPreferenceTag::getTagValue)
                            .last("LIMIT 5")
            );
            if (!categoryPrefs.isEmpty()) {
                profile.setCategoryTags(categoryPrefs.stream()
                        .map(UserPreferenceTag::getTagName).collect(Collectors.joining(",")));
            }
        } catch (Exception e) {
            log.warn("获取品类偏好失败", e);
        }
    }

    /**
     * 分析就餐方式偏好（基于订单中的 diningType 字段统计）
     */
    private String analyzePreferredDiningType(Long userId) {
        try {
            // 查询最近30天订单的就餐方式分布
            LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Orders::getUserId, userId)
                    .isNotNull(Orders::getSource)
                    .ge(Orders::getOrderTime, java.time.LocalDateTime.now().minusDays(30))
                    .last("LIMIT 100");
            List<Orders> orders = orderMapper.selectList(wrapper);

            if (orders.isEmpty()) return "delivery";

            Map<String, Long> typeCount = orders.stream()
                    .collect(Collectors.groupingBy(Orders::getSource, Collectors.counting()));

            return typeCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("delivery");
        } catch (Exception e) {
            return "delivery";
        }
    }

    /**
     * 分析点餐时段偏好
     */
    private String analyzePreferredTimeSlot(Long userId) {
        try {
            return preferenceAnalysisService.analyzeTimePreference(userId);
        } catch (Exception e) {
            return "lunch";
        }
    }

    /**
     * 追加历史反馈摘要
     */
    private void appendFeedbackSummary(Long userId, StringBuilder sb) {
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getUserId, userId)
                .isNotNull(AIMessageRecord::getFeedback)
                .orderByDesc(AIMessageRecord::getCreateTime)
                .last("LIMIT 50");
        List<AIMessageRecord> feedbacks = aiMessageRecordMapper.selectList(wrapper);

        if (feedbacks.isEmpty()) return;

        long goodCount = feedbacks.stream().filter(f -> "good".equals(f.getFeedback())).count();
        long badCount = feedbacks.stream().filter(f -> "bad".equals(f.getFeedback())).count();
        int total = feedbacks.size();

        if (total > 0) {
            sb.append("历史反馈：最近").append(total).append("条中 ")
              .append(goodCount).append("条觉得有用，").append(badCount).append("条觉得没用\n");
        }
    }

    // ==================== 统计方法 ====================

    private int countUserConversations(Long userId) {
        // 通过消息表估算对话数（每个用户消息算一条，粗略估算）
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getUserId, userId)
                .eq(AIMessageRecord::getRole, "user");
        return Math.max(aiMessageRecordMapper.selectCount(wrapper) / 2, 0);
    }

    private int countUserFeedbacks(Long userId) {
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getUserId, userId)
                .isNotNull(AIMessageRecord::getFeedback);
        return (int) aiMessageRecordMapper.selectCount(wrapper);
    }

    private BigDecimal calculateConfidence(UserProfile profile) {
        // 基于数据丰富度计算置信度
        BigDecimal confidence = new BigDecimal("0.1");
        if (profile.getTasteTags() != null && !profile.getTasteTags().isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.15"));
        }
        if (profile.getCategoryTags() != null && !profile.getCategoryTags().isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.15"));
        }
        if (profile.getFrequentDishIds() != null && !profile.getFrequentDishIds().isEmpty()) {
            confidence = confidence.add(new BigDecimal("0.2"));
        }
        if (profile.getTotalFeedbacks() != null && profile.getTotalFeedbacks() > 5) {
            confidence = confidence.add(new BigDecimal("0.2"));
        }
        if (profile.getTotalConversations() != null && profile.getTotalConversations() > 10) {
            confidence = confidence.add(new BigDecimal("0.2"));
        }
        return confidence.min(new BigDecimal("1.0"));
    }

    // ==================== 翻译辅助方法 ====================

    private String translatePricePref(String pref) {
        switch (pref) {
            case "budget": return "经济实惠型";
            case "economy": return "性价比优先";
            case "standard": return "中档消费";
            case "premium": return "品质优先";
            default: return pref;
        }
    }

    private String translateDiningType(String type) {
        switch (type) {
            case "delivery": return "外卖配送";
            case "dine_in": return "堂食";
            case "pickup": return "到店自取";
            default: return type;
        }
    }

    private String translateTimeSlot(String slot) {
        switch (slot) {
            case "morning": return "早餐时段";
            case "lunch": return "午餐时段";
            case "dinner": return "晚餐时段";
            case "late_night": return "夜宵时段";
            default: return slot;
        }
    }
}






