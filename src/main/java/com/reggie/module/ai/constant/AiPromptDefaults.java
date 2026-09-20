package com.reggie.module.ai.constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 提示词模板内置默认内容（P3）。
 * <p>来源搬迁：SYSTEM 原为 {@code AIConfigProperties} 写死常量（marketing 原为 AIChatServiceImpl 内联串），
 * WELCOME/QUICK 原为 AIChatController 静态 Map。</p>
 * <p>用途：①首次启动 Seeder 补插内置模板；②后台「重置默认」按 code 恢复原文。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public final class AiPromptDefaults {

    /** 模板类型：系统提示词（不下发给前端） */
    public static final String TYPE_SYSTEM = "SYSTEM";
    /** 模板类型：欢迎语 */
    public static final String TYPE_WELCOME = "WELCOME";
    /** 模板类型：快捷问题 */
    public static final String TYPE_QUICK = "QUICK";

    /** 全部合法场景（展示顺序即排序顺序） */
    public static final List<String> SCENES = Arrays.asList(
            "business_analysis", "dish_desc", "marketing", "order_assistant");

    /** 场景中文标签 */
    public static final Map<String, String> SCENE_LABELS = new LinkedHashMap<>();

    /** 全部合法类型（展示顺序） */
    public static final List<String> TYPES = Arrays.asList(TYPE_SYSTEM, TYPE_WELCOME, TYPE_QUICK);

    /** 类型中文标签 */
    public static final Map<String, String> TYPE_LABELS = new LinkedHashMap<>();

    /** code -> 默认模板（顺序即初始 sort） */
    private static final Map<String, TemplateDefault> DEFAULTS = new LinkedHashMap<>();

    static {
        SCENE_LABELS.put("business_analysis", "经营分析");
        SCENE_LABELS.put("dish_desc", "菜品描述");
        SCENE_LABELS.put("marketing", "营销文案");
        SCENE_LABELS.put("order_assistant", "点餐助手");

        TYPE_LABELS.put(TYPE_SYSTEM, "系统提示词");
        TYPE_LABELS.put(TYPE_WELCOME, "欢迎语");
        TYPE_LABELS.put(TYPE_QUICK, "快捷问题");

        // ===== SYSTEM：原 AIConfigProperties 三个默认值 + marketing 内联串 =====
        put(TYPE_SYSTEM, "order_assistant", "点餐助手 · 系统提示词",
                "你是一个专业的餐饮推荐助手，名叫「小吉」。"
                        + "你的任务是根据用户的需求和偏好，从当前门店的菜品中智能推荐最合适的菜品。\n"
                        + "规则：\n"
                        + "1. 只推荐门店真实存在的菜品，不要编造菜品\n"
                        + "2. 考虑用户的口味偏好、预算、人数等因素\n"
                        + "3. 推荐要多样化，荤素搭配\n"
                        + "4. 回复简洁友好，用中文\n"
                        + "5. 推荐理由要具体，说明为什么适合用户\n"
                        + "6. 输出格式为JSON数组，每个菜品包含：dishId（菜品ID）、name（菜名）、reason（推荐理由）",
                null);
        put(TYPE_SYSTEM, "dish_desc", "菜品描述 · 系统提示词",
                "你是一个专业的美食文案写手。"
                        + "请根据菜名和基本信息，生成一段吸引人的菜品描述。\n"
                        + "要求：\n"
                        + "1. 描述食材、口味、烹饪方式\n"
                        + "2. 语言生动诱人，适合外卖平台展示\n"
                        + "3. 长度控制在50-150字\n"
                        + "4. 返回纯文本，不要加任何标记",
                null);
        put(TYPE_SYSTEM, "business_analysis", "经营分析 · 系统提示词",
                "你是一个餐饮经营数据分析师。"
                        + "请根据提供的经营数据，回答用户关于经营状况的问题。\n"
                        + "要求：\n"
                        + "1. 基于数据事实回答，不要编造数据\n"
                        + "2. 给出具体数字和趋势分析\n"
                        + "3. 提供可行的经营建议\n"
                        + "4. 回复简洁专业",
                null);
        put(TYPE_SYSTEM, "marketing", "营销文案 · 系统提示词",
                "你是一个营销文案专家。请根据用户需求生成吸引人的营销文案。文案要有感染力，适合外卖平台推送。",
                null);

        // ===== WELCOME：原 AIChatController.SCENE_WELCOME =====
        put(TYPE_WELCOME, "business_analysis", "经营分析 · 欢迎语",
                "你好，我是你的经营分析助手，可以帮你分析营业额、热销菜品、客流时段等经营问题。", null);
        put(TYPE_WELCOME, "dish_desc", "菜品描述 · 欢迎语",
                "你好，告诉我菜品名称和主要食材，我可以帮你生成诱人的菜品描述。", null);
        put(TYPE_WELCOME, "marketing", "营销文案 · 欢迎语",
                "你好，描述你的活动内容和目标客群，我来帮你生成适合外卖平台推送的营销文案。", null);
        put(TYPE_WELCOME, "order_assistant", "点餐助手 · 欢迎语",
                "你好呀！我是点餐小助手，可以帮你推荐菜品、介绍口味和份量，有什么想吃的尽管问我～", null);

        // ===== QUICK：原 AIChatController.SCENE_QUICK_QUESTIONS =====
        put(TYPE_QUICK, "business_analysis", "经营分析 · 快捷问题", null,
                Arrays.asList("最近7天的营业额趋势怎么样？", "热销菜品 Top10 是哪些？",
                        "午市和晚市的销售占比如何？", "顾客的复购情况怎么样？"));
        put(TYPE_QUICK, "dish_desc", "菜品描述 · 快捷问题", null,
                Arrays.asList("帮我写一份宫保鸡丁的菜品描述", "生成一段麻辣香锅的外卖介绍", "鱼香肉丝怎么描述更吸引人？"));
        put(TYPE_QUICK, "marketing", "营销文案 · 快捷问题", null,
                Arrays.asList("写一条周末满减活动的推送文案", "新客首单立减活动怎么宣传？", "帮我写会员日充值活动文案"));
        put(TYPE_QUICK, "order_assistant", "点餐助手 · 快捷问题", null,
                Arrays.asList("今天有什么好吃的推荐？", "3个人吃饭点什么比较合适？",
                        "有什么不辣的菜吗？", "店里的人气招牌菜有哪些？"));
    }

    private AiPromptDefaults() {
    }

    private static void put(String type, String scene, String title, String content, List<String> quickQuestions) {
        DEFAULTS.put(code(type, scene), new TemplateDefault(type, scene, title, content, quickQuestions));
    }

    /** 模板编码：{type 小写}_{scene}，如 system_order_assistant / quick_marketing */
    public static String code(String type, String scene) {
        return type.toLowerCase() + "_" + scene;
    }

    public static boolean isValidScene(String scene) {
        return scene != null && SCENE_LABELS.containsKey(scene);
    }

    public static boolean isValidType(String type) {
        return TYPE_LABELS.containsKey(type);
    }

    public static String sceneLabel(String scene) {
        String label = SCENE_LABELS.get(scene);
        return label != null ? label : scene;
    }

    public static String typeLabel(String type) {
        String label = TYPE_LABELS.get(type);
        return label != null ? label : type;
    }

    /** 按 code 取内置默认；非内置 code 返回 null */
    public static TemplateDefault get(String code) {
        return DEFAULTS.get(code);
    }

    /** 全部内置默认（按场景 → 类型顺序） */
    public static List<TemplateDefault> all() {
        return new ArrayList<>(DEFAULTS.values());
    }

    /** 单条内置默认内容 */
    public static class TemplateDefault {
        private final String type;
        private final String scene;
        private final String title;
        private final String content;
        private final List<String> quickQuestions;

        TemplateDefault(String type, String scene, String title, String content, List<String> quickQuestions) {
            this.type = type;
            this.scene = scene;
            this.title = title;
            this.content = content;
            this.quickQuestions = quickQuestions;
        }

        public String getType() {
            return type;
        }

        public String getScene() {
            return scene;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public List<String> getQuickQuestions() {
            return quickQuestions;
        }
    }
}
