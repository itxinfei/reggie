package com.reggie.module.ai.service;

import com.reggie.module.ai.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多轮对话理解服务
 *
 * <p>核心能力：
 * <ul>
 *   <li>意图分类：将用户消息归类为已知意图类型</li>
 *   <li>指代消解：检测并解析"这个"、"刚才"等指代</li>
 *   <li>澄清追问：当消息信息不足时，主动询问补充</li>
 *   <li>上下文补全：根据历史对话推断隐含信息</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Service
public class DialogueIntentService {

    /** 意图类型 */
    public enum IntentType {
        /** 点餐推荐 */
        ORDER_RECOMMEND("点餐推荐"),
        /** 菜品详情查询 */
        DISH_QUERY("菜品查询"),
        /** 价格/优惠咨询 */
        PRICE_INQUIRY("价格咨询"),
        /** 配送/时间咨询 */
        DELIVERY_INQUIRY("配送咨询"),
        /** 评价/售后 */
        FEEDBACK("评价反馈"),
        /** 一般闲聊 */
        CHAT("一般闲聊"),
        /** 模糊意图，需澄清 */
        AMBIGUOUS("模糊意图");

        final String label;
        IntentType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** 意图匹配规则（关键词 → 意图） */
    private static final Map<IntentType, String[]> INTENT_KEYWORDS;

    static {
        INTENT_KEYWORDS = new HashMap<>();
        INTENT_KEYWORDS.put(IntentType.ORDER_RECOMMEND, new String[]{
                "推荐", "点餐", "吃什么", "来一份", "来点", "给我来", "帮我点", "建议", "菜单"
        });
        INTENT_KEYWORDS.put(IntentType.DISH_QUERY, new String[]{
                "介绍", "什么口味", "辣不辣", "甜不甜", "成分", "食材", "做法", "有没有", "有吗"
        });
        INTENT_KEYWORDS.put(IntentType.PRICE_INQUIRY, new String[]{
                "多少钱", "价格", "便宜", "贵", "优惠", "满减", "折扣", "券", "特价", "套餐"
        });
        INTENT_KEYWORDS.put(IntentType.DELIVERY_INQUIRY, new String[]{
                "多久到", "配送", "快递", "外卖", "配送费", "起送", "自取", "取餐"
        });
        INTENT_KEYWORDS.put(IntentType.FEEDBACK, new String[]{
                "不好吃", "投诉", "退款", "差评", "太咸", "太辣", "凉了", "洒了", "不满意"
        });
        INTENT_KEYWORDS.put(IntentType.CHAT, new String[]{
                "你好", "谢谢", "再见", "哈喽", "嗨", "在吗", "辛苦了", "不错", "可以"
        });
    }

    /** 指代消解关键词 */
    private static final Pattern COREFERENCE_PATTERN = Pattern.compile(
            "(这个|那个|它|刚才|刚刚|上面|之前|这家|那个店|这个菜|你说的|推荐的)"
    );

    /** 单轮对话状态（按会话缓存） */
    private final Map<String, DialogueState> dialogueStates = new ConcurrentHashMap<>();

    // ==================== 公共 API ====================

    /**
     * 分析用户消息，返回意图分析结果
     */
    public IntentAnalysis analyze(String conversationId, String userMessage,
                                   List<AIMessage> recentHistory) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return IntentAnalysis.unknown();
        }

        DialogueState state = dialogueStates.computeIfAbsent(conversationId, k -> new DialogueState());

        IntentType intent = classify(userMessage);
        String resolvedMessage = userMessage;
        boolean hasCoreference = detectCoreference(userMessage);

        // 指代消解
        if (hasCoreference && recentHistory != null && !recentHistory.isEmpty()) {
            resolvedMessage = resolveCoreference(userMessage, recentHistory);
            log.debug("指代消解: '{}' -> '{}'", userMessage, resolvedMessage);
        }

        // 检查是否需要澄清
        java.util.List<String> clarifications = new ArrayList<>();
        boolean needsClarification = false;
        if (intent == IntentType.AMBIGUOUS) {
            needsClarification = true;
            clarifications.add(generateClarificationForAmbiguous(userMessage));
        }

        // 更新对话状态
        state.lastIntent = intent;
        state.lastMessage = userMessage;
        state.turnCount++;
        state.lastActive = System.currentTimeMillis();

        return new IntentAnalysis(intent, userMessage, resolvedMessage,
                hasCoreference, needsClarification, clarifications, calculateConfidence(intent, userMessage));
    }

    /**
     * 获取补充信息提示
     */
    public String generateSupplementHint(String conversationId, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return null;
        }
        StringBuilder hint = new StringBuilder();
        if (userMessage.contains("推荐") && !userMessage.contains("什么")) {
            hint.append("【补充提示】用户已请求推荐，但未说明口味偏好。建议主动询问：'您喜欢什么口味？辣度偏好？预算范围？'");
        } else if (userMessage.contains("多少钱") && userMessage.length() < 5) {
            hint.append("【补充提示】用户询问价格但未指明具体菜品。建议追问：'请问您想了解哪道菜的价格？'");
        }
        return hint.length() > 0 ? hint.toString() : null;
    }

    /**
     * 获取对话状态统计
     */
    public Map<String, Object> getDialogueStats(String conversationId) {
        if (conversationId == null) {
            return Collections.emptyMap();
        }
        DialogueState state = dialogueStates.get(conversationId);
        if (state == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("intent", state.lastIntent != null ? state.lastIntent.getLabel() : null);
        stats.put("turnCount", state.turnCount);
        stats.put("lastActive", new java.util.Date(state.lastActive));
        return stats;
    }

    /**
     * 清除对话状态
     */
    public void clearState(String conversationId) {
        if (conversationId != null) {
            dialogueStates.remove(conversationId);
        }
    }

    // ==================== 意图分类 ====================

    private IntentType classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return IntentType.AMBIGUOUS;
        }
        String lower = message.toLowerCase();

        Map<IntentType, Integer> scores = new HashMap<>();
        for (Map.Entry<IntentType, String[]> entry : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) {
                    score++;
                }
            }
            if (score > 0) {
                scores.put(entry.getKey(), score);
            }
        }

        if (scores.isEmpty()) {
            if (message.length() < 3) {
                return IntentType.AMBIGUOUS;
            }
            return IntentType.CHAT;
        }

        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();
    }

    private double calculateConfidence(IntentType intent, String message) {
        if (intent == IntentType.AMBIGUOUS) {
            return message.length() >= 3 ? 0.3 : 0.1;
        }
        if (intent == IntentType.CHAT && message.length() <= 4) {
            return 0.7;
        }
        return 0.85;
    }

    // ==================== 指代消解 ====================

    private boolean detectCoreference(String message) {
        if (message == null) return false;
        Matcher matcher = COREFERENCE_PATTERN.matcher(message);
        return matcher.find();
    }

    private String resolveCoreference(String message, List<AIMessage> history) {
        String resolved = message;
        if (history != null && !history.isEmpty()) {
            AIMessage lastAssistant = null;
            for (int i = history.size() - 1; i >= 0; i--) {
                if ("assistant".equals(history.get(i).getRole())) {
                    lastAssistant = history.get(i);
                    break;
                }
            }
            if (lastAssistant != null && lastAssistant.getContent() != null) {
                List<String> entities = extractEntities(lastAssistant.getContent());
                if (!entities.isEmpty()) {
                    resolved = resolved.replaceAll("这个", entities.get(0));
                    resolved = resolved.replaceAll("那个", entities.size() > 1 ? entities.get(1) : entities.get(0));
                    resolved = resolved.replaceAll("它", entities.get(0));
                }
            }
        }
        return resolved;
    }

    private List<String> extractEntities(String text) {
        List<String> entities = new ArrayList<>();
        if (text == null) return entities;

        Pattern p = Pattern.compile("[\\d）)】]\\s*([^\\n]{2,10})");
        Matcher m = p.matcher(text);
        while (m.find() && entities.size() < 3) {
            String name = m.group(1).trim();
            if (name.length() >= 2 && name.length() <= 10) {
                entities.add(name);
            }
        }

        if (entities.isEmpty()) {
            p = Pattern.compile("\"([^\"]{2,10})\"|《([^》]{2,10})》|「([^」]{2,10})」");
            m = p.matcher(text);
            while (m.find() && entities.size() < 3) {
                String found = m.group();
                int start = found.charAt(0) == '「' ? 1 : (found.charAt(0) == '《' ? 1 : (found.charAt(0) == '“' ? 1 : 0));
                int end = found.charAt(found.length() - 1) == '」' ? found.length() - 1 : (found.charAt(found.length() - 1) == '》' ? found.length() - 1 : (found.charAt(found.length() - 1) == '”' ? found.length() - 1 : found.length()));
                entities.add(found.substring(start, end));
            }
        }

        return entities;
    }

    // ==================== 澄清追问 ====================

    private String generateClarificationForAmbiguous(String message) {
        if (message.length() < 2) {
            return "您想了解什么呢？可以告诉我您的需求，比如推荐菜品、查询价格或了解配送信息。";
        }
        if (message.contains("？") || message.contains("?")) {
            return "您的问题我可能没有完全理解。能再详细说说吗？比如您想了解什么菜品、或者需要什么帮助？";
        }
        return "我不太确定您的意思，您是想点餐、查询菜品信息，还是有其他问题？";
    }

    // ==================== 内部状态 ====================

    private static class DialogueState {
        IntentType lastIntent;
        String lastMessage;
        int turnCount;
        long lastActive = System.currentTimeMillis();
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dialogue-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::cleanupExpired, 30, 30, java.util.concurrent.TimeUnit.MINUTES);
    }

    private synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        long expireMillis = 24 * 60 * 60 * 1000L;
        int cleaned = 0;
        Iterator<Map.Entry<String, DialogueState>> it = dialogueStates.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastActive > expireMillis) {
                it.remove();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.debug("清理过期对话状态: count={}", cleaned);
        }
    }

    // ==================== IntentAnalysis 数据类 ====================

    public static class IntentAnalysis {
        private IntentType intent;
        private String originalMessage;
        private String resolvedMessage;
        private boolean hasCoreference;
        private boolean needsClarification;
        private java.util.List<String> clarificationQuestions;
        private double confidence;

        public IntentAnalysis() {}

        public IntentAnalysis(IntentType intent, String originalMessage, String resolvedMessage,
                              boolean hasCoreference, boolean needsClarification,
                              java.util.List<String> clarificationQuestions, double confidence) {
            this.intent = intent;
            this.originalMessage = originalMessage;
            this.resolvedMessage = resolvedMessage;
            this.hasCoreference = hasCoreference;
            this.needsClarification = needsClarification;
            this.clarificationQuestions = clarificationQuestions;
            this.confidence = confidence;
        }

        public static IntentAnalysis unknown() {
            IntentAnalysis r = new IntentAnalysis();
            r.intent = IntentType.AMBIGUOUS;
            r.confidence = 0.1;
            return r;
        }

        public IntentType getIntent() { return intent; }
        public String getOriginalMessage() { return originalMessage; }
        public String getResolvedMessage() { return resolvedMessage; }
        public boolean isHasCoreference() { return hasCoreference; }
        public boolean isNeedsClarification() { return needsClarification; }
        public java.util.List<String> getClarificationQuestions() { return clarificationQuestions; }
        public double getConfidence() { return confidence; }
    }
}
