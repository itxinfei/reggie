package com.reggie.module.ai.service;

import com.reggie.module.ai.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话上下文记忆管理服务
 *
 * <p>核心能力：
 * <ul>
 *   <li>滑动窗口：保留最近 N 条消息，控制 token 消耗</li>
 *   <li>关键信息提取：从消息中提取用户偏好、约束条件、实体引用</li>
 *   <li>上下文压缩：当消息过多时，将早期消息压缩为摘要</li>
 *   <li>上下文重建：从摘要 + 滑动窗口重建完整上下文</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
public class ConversationContextService {

    /** 滑动窗口大小（保留最近的消息条数） */
    private static final int SLIDING_WINDOW_SIZE = 12;

    /** 触发压缩的阈值（超过此条数开始压缩早期消息） */
    private static final int COMPRESSION_THRESHOLD = 20;

    /** 每条消息估算 token（中文约 1.5 token/字，英文约 0.25 token/词） */
    private static final int CHARS_PER_TOKEN = 2;

    /** 单轮上下文最大 token 预算 */
    private static final int MAX_CONTEXT_TOKENS = 3000;

    /** 对话上下文缓存（conversationId → ContextState） */
    private final Map<String, ContextState> contextCache = new ConcurrentHashMap<>();

    /** 用户偏好关键词集合（用于提取关键信息） */
    private static final Set<String> PREFERENCE_KEYWORDS = new HashSet<>(Arrays.asList(
            "喜欢", "讨厌", "偏好", "不要", "必须", "必须是", "不能", "不要辣",
            "微辣", "中辣", "重辣", "清淡", "咸一点", "甜一点", "酸一点",
            "vegetarian", "素食", "清真", "halal", "allergy", "过敏"
    ));

    /** 约束条件关键词 */
    private static final Set<String> CONSTRAINT_KEYWORDS = new HashSet<>(Arrays.asList(
            "预算", "不超过", "最多", "最低", "至少", "范围", "时间", "多久",
            "配送费", "起送", "满减", "优惠"
    ));

    // ==================== 公共 API ====================

    /**
     * 获取对话的上下文（滑动窗口 + 摘要）
     *
     * @param conversationId 会话ID
     * @return 上下文消息列表（含可能的摘要 system 消息）
     */
    public List<AIMessage> getContext(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }
        ContextState state = contextCache.get(conversationId);
        if (state == null || state.messages.isEmpty()) {
            return Collections.emptyList();
        }
        return state.buildContext();
    }

    /**
     * 添加消息到上下文
     *
     * @param conversationId 会话ID
     * @param role           角色（user / assistant）
     * @param content        消息内容
     */
    public void addMessage(String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isEmpty()
                || content == null || content.isEmpty()) {
            return;
        }
        ContextState state = contextCache.computeIfAbsent(conversationId, k -> new ContextState());
        AIMessage msg = AIMessage.builder()
                .role(role)
                .content(content)
                .build();
        state.messages.add(msg);

        // 提取关键信息
        if ("user".equals(role)) {
            state.keyFacts.addAll(extractKeyFacts(content));
        }

        // 触发压缩
        if (state.messages.size() > COMPRESSION_THRESHOLD) {
            compress(state);
        }

        // 清理过期上下文（24小时无活动）
        state.lastActive = System.currentTimeMillis();
    }

    /**
     * 获取对话的关键信息摘要（用于前端展示或日志）
     *
     * @param conversationId 会话ID
     * @return 关键信息列表，无上下文时返回空列表
     */
    public List<String> getKeyFacts(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyList();
        }
        ContextState state = contextCache.get(conversationId);
        return state != null ? new ArrayList<>(state.keyFacts) : Collections.emptyList();
    }

    /**
     * 清空对话上下文（对话重置时调用）
     *
     * @param conversationId 会话ID
     */
    public void clearContext(String conversationId) {
        if (conversationId != null) {
            contextCache.remove(conversationId);
            log.debug("已清空对话上下文: conversationId={}", conversationId);
        }
    }

    /**
     * 重建上下文（从消息记录加载历史）
     *
     * @param conversationId 会话ID
     * @param historyMessages 历史消息列表
     */
    public void rebuild(String conversationId, List<AIMessage> historyMessages) {
        if (conversationId == null || historyMessages == null || historyMessages.isEmpty()) {
            return;
        }
        ContextState state = contextCache.computeIfAbsent(conversationId, k -> new ContextState());
        state.messages.clear();
        state.keyFacts.clear();
        state.summary = null;

        for (AIMessage msg : historyMessages) {
            state.messages.add(msg);
            if ("user".equals(msg.getRole()) && msg.getContent() != null) {
                state.keyFacts.addAll(extractKeyFacts(msg.getContent()));
            }
        }
        state.lastActive = System.currentTimeMillis();

        // 如果历史消息过多，立即压缩
        if (state.messages.size() > COMPRESSION_THRESHOLD) {
            compress(state);
        }
        log.debug("已重建对话上下文: conversationId={}, messages={}, keyFacts={}",
                conversationId, state.messages.size(), state.keyFacts.size());
    }

    /**
     * 获取上下文统计信息（用于监控和调试）
     */
    public Map<String, Object> getStats(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyMap();
        }
        ContextState state = contextCache.get(conversationId);
        if (state == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMessages", state.messages.size());
        stats.put("keyFacts", state.keyFacts.size());
        stats.put("hasSummary", state.summary != null);
        stats.put("lastActive", new Date(state.lastActive));
        return stats;
    }

    // ==================== 关键信息提取 ====================

    /**
     * 从用户消息中提取关键信息（偏好、约束、实体引用）
     */
    private List<String> extractKeyFacts(String content) {
        List<String> facts = new ArrayList<>();
        if (content == null || content.length() < 2) {
            return facts;
        }

        String lowerContent = content.toLowerCase();

        // 提取偏好
        for (String keyword : PREFERENCE_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                facts.add("PREF:" + keyword);
            }
        }

        // 提取约束
        for (String keyword : CONSTRAINT_KEYWORDS) {
            if (lowerContent.contains(keyword)) {
                facts.add("CONSTRAINT:" + keyword);
            }
        }

        // 提取菜品引用（数字+文字的常见模式）
        String[] segments = content.split("[，,。.！!？?\\s]+");
        for (String seg : segments) {
            String trimmed = seg.trim();
            if (trimmed.length() >= 2 && trimmed.length() <= 20
                    && !trimmed.equalsIgnoreCase("好的")
                    && !trimmed.equalsIgnoreCase("谢谢")
                    && !trimmed.equalsIgnoreCase("不用")) {
                facts.add("ENTITY:" + trimmed);
            }
        }

        return facts;
    }

    // ==================== 上下文压缩 ====================

    /**
     * 压缩上下文：将早期消息合成为摘要，保留滑动窗口
     */
    private synchronized void compress(ContextState state) {
        if (state.messages.size() <= COMPRESSION_THRESHOLD) {
            return;
        }

        int windowStart = Math.max(0, state.messages.size() - SLIDING_WINDOW_SIZE);
        List<AIMessage> toCompress = new ArrayList<>(state.messages.subList(0, windowStart));
        List<AIMessage> keepMessages = new ArrayList<>(state.messages.subList(windowStart, state.messages.size()));

        // 生成摘要
        String newSummary = generateSummary(toCompress, state.summary);
        state.summary = newSummary;
        state.messages = keepMessages;

        log.debug("上下文压缩完成: conversationId cached, compressed={} → window={}, summaryLength={}",
                toCompress.size(), keepMessages.size(),
                newSummary != null ? newSummary.length() : 0);
    }

    /**
     * 生成消息摘要（基于规则的关键内容提取）
     *
     * <p>不调用 LLM，纯规则提取，避免额外 token 消耗。
     * 保留：用户偏好、菜品/实体引用、决策结果。
     */
    private String generateSummary(List<AIMessage> oldMessages, String existingSummary) {
        StringBuilder sb = new StringBuilder();
        if (existingSummary != null && !existingSummary.isEmpty()) {
            sb.append("[之前摘要] ").append(existingSummary).append("\n");
        }

        Set<String> userPreferences = new LinkedHashSet<>();
        Set<String> entities = new LinkedHashSet<>();
        StringBuilder lastAction = new StringBuilder();

        for (AIMessage msg : oldMessages) {
            if ("user".equals(msg.getRole())) {
                List<String> facts = extractKeyFacts(msg.getContent());
                for (String fact : facts) {
                    if (fact.startsWith("PREF:")) {
                        userPreferences.add(fact.substring(5));
                    } else if (fact.startsWith("ENTITY:")) {
                        entities.add(fact.substring(7));
                    }
                }
                if (msg.getContent() != null && msg.getContent().length() > 5) {
                    lastAction.setLength(0);
                    lastAction.append(msg.getContent(), 0, Math.min(30, msg.getContent().length()));
                }
            } else if ("assistant".equals(msg.getRole())) {
                // 提取 assistant 的决策（包含推荐/建议的响应）
                if (msg.getContent() != null
                        && (msg.getContent().contains("推荐") || msg.getContent().contains("建议"))) {
                    lastAction.setLength(0);
                    lastAction.append(msg.getContent(), 0, Math.min(50, msg.getContent().length()));
                }
            }
        }

        if (!userPreferences.isEmpty()) {
            sb.append("用户偏好：").append(String.join("、", userPreferences)).append("。\n");
        }
        if (!entities.isEmpty()) {
            sb.append("相关实体：").append(String.join("、", entities)).append("。\n");
        }
        if (lastAction.length() > 0) {
            sb.append("上次对话：").append(lastAction.toString()).append("...");
        }

        String summary = sb.toString().trim();
        return summary.isEmpty() ? null : summary;
    }

    // ==================== 内部状态 ====================

    /**
     * 单个对话的上下文状态
     */
    private static class ContextState {
        /** 消息列表（原始消息，未压缩） */
        List<AIMessage> messages = new ArrayList<>();

        /** 提取的关键信息集合 */
        Set<String> keyFacts = new LinkedHashSet<>();

        /** 压缩摘要（早期消息的精简概括） */
        String summary;

        /** 最后活跃时间 */
        long lastActive = System.currentTimeMillis();

        /** 构建上下文消息列表（摘要 + 滑动窗口） */
        List<AIMessage> buildContext() {
            List<AIMessage> result = new ArrayList<>();
            if (summary != null && !summary.isEmpty()) {
                result.add(AIMessage.builder().role("system").content("[对话摘要]\n" + summary).build());
            }
            result.addAll(messages);
            return result;
        }
    }

    // ==================== 生命周期 ====================

    /**
     * 定时清理过期上下文（每 30 分钟清理 24 小时前的数据）
     */
    @PostConstruct
    public void init() {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "context-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::cleanupExpired, 30, 30, java.util.concurrent.TimeUnit.MINUTES);
    }

    private synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        long expireMillis = 24 * 60 * 60 * 1000L; // 24 hours
        int cleaned = 0;
        Iterator<Map.Entry<String, ContextState>> it = contextCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ContextState> entry = it.next();
            if (now - entry.getValue().lastActive > expireMillis) {
                it.remove();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.debug("清理过期对话上下文: count={}", cleaned);
        }
    }
}
