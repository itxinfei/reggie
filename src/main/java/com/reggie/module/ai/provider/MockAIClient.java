package com.reggie.module.ai.provider;

import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模拟AI Provider（无需API Key即可使用）
 * 修改点：添加@Primary作为默认Bean，仅当AI未显式启用或未配置API Key时激活
 * 用于演示、测试或无网络环境
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "reggie.ai.enabled", havingValue = "false", matchIfMissing = true)
public class MockAIClient implements AIClient {

    /** AI配置属性 */
    @Resource
    private AIConfigProperties aiConfig;

    /**
     * 发送聊天请求（模拟实现）
     *
     * @param messages   消息列表（system/user/assistant）
     * @param maxTokens  最大返回Token数
     * @param temperature 温度参数
     * @return AI响应
     */
    @Override
    public AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature) {
        log.info("Mock AI响应: messagesCount={}", messages.size());

        String userMsg = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                userMsg = messages.get(i).getContent();
                break;
            }
        }

        String reply;
        if (userMsg.contains("推荐") || userMsg.contains("吃什么") || userMsg.contains("点菜")) {
            reply = generateRecommendReply(userMsg);
        } else if (userMsg.contains("分析") || userMsg.contains("报表") || userMsg.contains("经营")) {
            reply = "您好！我是经营分析助手。目前处于演示模式，连接AI服务后可以提供详细的经营分析。\n\n"
                    + "建议：\n"
                    + "1. 关注近期热销菜品排行\n"
                    + "2. 对比同时段历史数据\n"
                    + "3. 分析客单价变化趋势";
        } else if (userMsg.contains("描述") || userMsg.contains("文案")) {
            reply = "「精选食材，匠心烹饪」这道菜品选用新鲜食材，经过大厨精心烹制，"
                    + "口感丰富层次分明，色香味俱全，是您不可错过的美味之选。\n\n"
                    + "💡 提示：连接AI服务可生成更专业的美食文案。";
        } else {
            reply = "您好！我是「小吉」，瑞吉外卖的AI助手。\n\n"
                    + "我可以帮您：\n"
                    + "🍜 智能点餐推荐 - 告诉我您想吃什么\n"
                    + "📝 菜品描述生成 - 一键生成美食文案\n"
                    + "📊 经营数据分析 - 帮您了解经营状况\n\n"
                    + "💡 当前为演示模式，连接AI服务可获取更智能的回复。";
        }

        return AIChatResponse.builder()
                .content(reply)
                .model("mock-v1")
                .tokensUsed(0)
                .build();
    }

    /**
     * 生成模拟推荐回复
     */
    private String generateRecommendReply(String userMsg) {
        if (userMsg.contains("辣")) {
            return "为您推荐以下辣味菜品：\n"
                    + "🔥 麻辣香锅 - 麻辣鲜香，配料丰富\n"
                    + "🌶️ 水煮鱼 - 麻辣过瘾，鱼肉嫩滑\n"
                    + "🍗 辣子鸡 - 香辣酥脆，下饭神器\n\n"
                    + "💡 连接AI服务可获取基于您口味偏好的智能推荐。";
        } else if (userMsg.contains("清淡") || userMsg.contains("小孩")) {
            return "为您推荐以下清淡菜品：\n"
                    + "🥬 清炒时蔬 - 新鲜时令，清爽可口\n"
                    + "🍲 番茄蛋汤 - 酸甜开胃，营养丰富\n"
                    + "🐟 清蒸鲈鱼 - 鲜嫩不腻，老少皆宜\n\n"
                    + "💡 连接AI服务可获取基于您口味偏好的智能推荐。";
        } else {
            return "根据您的需求，为您推荐：\n"
                    + "🥩 红烧肉 - 肥而不腻，入口即化\n"
                    + "🥬 蒜蓉西兰花 - 清脆爽口，营养健康\n"
                    + "🍚 蛋炒饭 - 粒粒分明，家常美味\n\n"
                    + "💡 连接AI服务可获取基于您口味偏好的智能推荐。";
        }
    }

    /**
     * 获取提供商名称
     *
     * @return 提供商标识
     */
    @Override
    public String getProviderName() {
        return "mock";
    }

    /**
     * 获取默认模型名称
     *
     * @return 模型名称
     */
    @Override
    public String getDefaultModel() {
        return "mock-v1";
    }
}
