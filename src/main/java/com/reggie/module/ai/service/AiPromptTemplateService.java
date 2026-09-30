package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.AiPromptTemplate;
import com.reggie.module.ai.model.AiPromptTemplateHistory;

import java.util.List;

/**
 * AI 提示词模板服务（P3）
 * <p>聊天链路读取：{@link #getSystemPrompt(String)}、{@link #getWelcome(String)}、
 * {@link #getQuickQuestions(String)}，任何异常静默降级（返回 null/空列表），不阻断聊天。</p>
 * <p>P5 运营闭环：更新/重置/回滚前自动快照历史版本（{@link #listHistory(Long)} 可查、
 * {@link #rollback(Long, Long)} 可回滚）；生效模板带短 TTL 缓存，写操作即时失效。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface AiPromptTemplateService extends IService<AiPromptTemplate> {

    /** 后台分页：场景/类型精确过滤，关键词匹配标题或编码 */
    Page<AiPromptTemplate> adminPage(int page, int pageSize, String scene, String type, String keyword);

    /** 系统提示词（永不下发给前端）；无启用模板或异常时返回 null，调用方降级 */
    String getSystemPrompt(String scene);

    /** 场景欢迎语；无启用模板或异常时返回 null */
    String getWelcome(String scene);

    /** 场景快捷问题；无启用模板或异常时返回空列表 */
    List<String> getQuickQuestions(String scene);

    /** 新增自定义模板（同场景同类型已存在模板时拒绝） */
    Long createTemplate(AiPromptTemplate template);

    /** 更新模板；内置模板仅允许改标题/内容/快捷问题/启用状态/排序；更新前自动快照历史 */
    void updateTemplate(AiPromptTemplate template);

    /** 删除模板；内置模板拒绝删除；删除前自动快照历史 */
    void deleteTemplate(Long id);

    /** 内置模板重置为默认内容；重置前自动快照历史 */
    void resetBuiltin(Long id);

    /** 首次启动补插缺失的内置模板（已存在的 code 不动，保留运营修改） */
    void seedBuiltinsIfMissing();

    /** 模板历史版本列表（最近 20 条，新→旧） */
    List<AiPromptTemplateHistory> listHistory(Long templateId);

    /** 回滚到指定历史版本：当前内容先快照，再回写历史内容并递增版本 */
    void rollback(Long templateId, Long historyId);
}
