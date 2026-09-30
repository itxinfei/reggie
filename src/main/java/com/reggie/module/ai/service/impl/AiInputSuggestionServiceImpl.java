package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.ai.constant.AiPromptDefaults;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.model.AiChatConstants;
import com.reggie.module.ai.service.AiInputSuggestionService;
import com.reggie.module.ai.service.AiPromptTemplateService;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.dish.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 智能输入提示服务实现。
 * <p>候选源按优先级合并去重：快捷问题(quick) > 菜品(dish) > 个人历史(mine) > 店铺热点(hot)。
 * 全部为本地数据查询（无 LLM 调用），保证联想响应在毫秒级且零 token 成本。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
@Slf4j
@Service
public class AiInputSuggestionServiceImpl implements AiInputSuggestionService {

    /** 返回候选总数上限 */
    public static final int MAX_SUGGESTIONS = 8;

    /** 各候选源单次条数上限 */
    private static final int PER_SOURCE_LIMIT = 3;

    /** 热点统计窗口（天） */
    private static final int HOT_WINDOW_DAYS = 30;

    /** 输入前缀最大长度（超长截断，防恶意长串） */
    private static final int MAX_PREFIX_LENGTH = 50;

    /** 门店热点/个人历史候选的最短长度（避开"好的""谢谢"这类无信息量短语） */
    private static final int MIN_CONTENT_LENGTH = 4;

    @Resource
    private AiPromptTemplateService promptTemplateService;

    @Resource
    private AIMessageRecordMapper messageRecordMapper;

    @Resource
    private DishMapper dishMapper;

    @Override
    public List<Map<String, Object>> suggest(Long userId, String actorType, String scene, String q) {
        // 场景收敛：员工可用三个后台场景，C 端固定点餐场景（与 /scene-config 同规则）
        boolean employee = AiChatConstants.ACTOR_EMPLOYEE.equals(actorType);
        String resolvedScene;
        if (employee) {
            resolvedScene = "order_assistant".equals(scene) || !AiPromptDefaults.isValidScene(scene)
                    ? "business_analysis" : scene;
        } else {
            resolvedScene = "order_assistant".equals(scene) ? scene : "order_assistant";
        }

        // 输入为空：返回场景快捷问题作为起步候选（聚焦输入框即出现）
        String prefix = normalizePrefix(q);
        if (prefix.isEmpty()) {
            List<Map<String, Object>> starter = new ArrayList<>();
            for (String question : asCandidates(promptTemplateService.getQuickQuestions(resolvedScene),
                    PER_SOURCE_LIMIT + 1)) {
                addCandidate(starter, new LinkedHashSet<String>(), question, "quick");
            }
            return starter;
        }

        Set<String> seen = new LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();

        // 1) 快捷问题：运营配置，前缀或包含命中优先（curated，权重最高）
        for (String question : promptTemplateService.getQuickQuestions(resolvedScene)) {
            if (question != null && (question.startsWith(prefix) || question.contains(prefix))) {
                addCandidate(result, seen, question, "quick");
            }
        }

        // 2) 菜品名（仅点餐场景）：在售菜品按前缀匹配
        if ("order_assistant".equals(resolvedScene)) {
            for (String dishName : matchDishNames(prefix)) {
                addCandidate(result, seen, "我想吃" + dishName + "，帮我推荐", "dish");
            }
        }

        // 3) 个人历史：本人最近提过的相似问题
        if (userId != null) {
            try {
                for (String content : messageRecordMapper.selectPersonalSuggestions(
                        userId, BaseContext.getCurrentTenantId(), escapeLike(prefix), PER_SOURCE_LIMIT)) {
                    addCandidate(result, seen, content, "mine");
                }
            } catch (Exception e) {
                log.debug("个人历史联想查询失败: {}", e.getMessage());
            }
        }

        // 4) 店铺热点：租户内近 30 天高频提问（热度加权）
        try {
            for (String content : messageRecordMapper.selectHotSuggestions(
                    BaseContext.getCurrentTenantId(), escapeLike(prefix),
                    LocalDateTime.now().minusDays(HOT_WINDOW_DAYS), PER_SOURCE_LIMIT + 2)) {
                addCandidate(result, seen, content, "hot");
            }
        } catch (Exception e) {
            log.debug("店铺热点联想查询失败: {}", e.getMessage());
        }

        return result.size() > MAX_SUGGESTIONS ? new ArrayList<>(result.subList(0, MAX_SUGGESTIONS)) : result;
    }

    /** 在售菜品按前缀匹配，最多 PER_SOURCE_LIMIT 条；任何异常静默返回空 */
    private List<String> matchDishNames(String prefix) {
        try {
            LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<>();
            qw.select(Dish::getName)
                    .eq(Dish::getStatus, 1)
                    .likeRight(Dish::getName, escapeLike(prefix))
                    .last("LIMIT " + PER_SOURCE_LIMIT);
            List<Dish> dishes = dishMapper.selectList(qw);
            List<String> names = new ArrayList<>();
            if (dishes != null) {
                for (Dish d : dishes) {
                    if (d.getName() != null && !d.getName().trim().isEmpty()) {
                        names.add(d.getName().trim());
                    }
                }
            }
            return names;
        } catch (Exception e) {
            log.debug("菜品联想查询失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String normalizePrefix(String q) {
        if (q == null) {
            return "";
        }
        String trimmed = q.trim();
        return trimmed.length() > MAX_PREFIX_LENGTH ? trimmed.substring(0, MAX_PREFIX_LENGTH) : trimmed;
    }

    /** LIKE 前缀转义：\ % _ 三个通配/转义字符 */
    private String escapeLike(String prefix) {
        return prefix.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<String> asCandidates(List<String> source, int limit) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.size() > limit ? source.subList(0, limit) : source;
    }

    /** 去重追加候选（LinkedHashSet 保序去重 + 数量封顶） */
    private void addCandidate(List<Map<String, Object>> result, Set<String> seen, String text, String source) {
        if (text == null || text.trim().isEmpty() || !seen.add(text.trim())) {
            return;
        }
        if (result.size() >= MAX_SUGGESTIONS) {
            return;
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("text", text.trim());
        item.put("source", source);
        result.add(item);
    }
}
