package com.reggie.module.ai.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.module.ai.constant.AiPromptDefaults;
import com.reggie.module.ai.mapper.AiPromptTemplateMapper;
import com.reggie.module.ai.model.AiPromptTemplate;
import com.reggie.module.ai.service.AiPromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 提示词模板服务实现（P3）
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
public class AiPromptTemplateServiceImpl
        extends ServiceImpl<AiPromptTemplateMapper, AiPromptTemplate>
        implements AiPromptTemplateService {

    /** 单条快捷问题上限条数 */
    private static final int MAX_QUICK_QUESTIONS = 8;
    /** 单个快捷问题最大长度 */
    private static final int MAX_QUESTION_LENGTH = 50;
    /** SYSTEM/WELCOME 内容最大长度 */
    private static final int MAX_CONTENT_LENGTH = 4000;
    /** 自定义模板默认排序 */
    private static final int CUSTOM_DEFAULT_SORT = 100;

    @Override
    public Page<AiPromptTemplate> adminPage(int page, int pageSize, String scene, String type, String keyword) {
        LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        if (scene != null && !scene.trim().isEmpty()) {
            wrapper.eq(AiPromptTemplate::getScene, scene.trim());
        }
        if (type != null && !type.trim().isEmpty()) {
            wrapper.eq(AiPromptTemplate::getType, type.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AiPromptTemplate::getTitle, kw).or().like(AiPromptTemplate::getCode, kw));
        }
        wrapper.orderByAsc(AiPromptTemplate::getSort).orderByAsc(AiPromptTemplate::getId);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public String getSystemPrompt(String scene) {
        AiPromptTemplate t = findEffective(scene, AiPromptDefaults.TYPE_SYSTEM);
        return t != null ? t.getContent() : null;
    }

    @Override
    public String getWelcome(String scene) {
        AiPromptTemplate t = findEffective(scene, AiPromptDefaults.TYPE_WELCOME);
        return t != null ? t.getContent() : null;
    }

    @Override
    public List<String> getQuickQuestions(String scene) {
        AiPromptTemplate t = findEffective(scene, AiPromptDefaults.TYPE_QUICK);
        if (t == null || t.getQuickQuestions() == null || t.getQuickQuestions().trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSONUtil.toList(t.getQuickQuestions(), String.class);
            return list != null ? list : Collections.<String>emptyList();
        } catch (Exception e) {
            log.warn("解析场景快捷问题失败 scene={}, 降级空列表: {}", scene, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTemplate(AiPromptTemplate template) {
        normalizeAndValidate(template, false);
        // 同场景同类型只允许一条模板：12 个内置槽位已覆盖全部组合，重复请直接编辑现有模板
        Long exists = countBySceneType(template.getScene(), template.getType(), null);
        if (exists != null && exists > 0) {
            throw new CustomException("该场景的" + AiPromptDefaults.typeLabel(template.getType())
                    + "模板已存在，请直接编辑现有模板");
        }
        template.setId(null);
        template.setCode(AiPromptDefaults.code(template.getType(), template.getScene()));
        template.setBuiltin(false);
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getSort() == null) {
            template.setSort(CUSTOM_DEFAULT_SORT);
        }
        template.setVersion(1);
        this.save(template);
        log.info("新增AI提示词模板: code={}, title={}", template.getCode(), template.getTitle());
        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(AiPromptTemplate template) {
        if (template.getId() == null) {
            throw new CustomException("模板ID不能为空");
        }
        AiPromptTemplate existing = this.getById(template.getId());
        if (existing == null) {
            throw new CustomException("提示词模板不存在");
        }
        // 内置模板：code/scene/type/builtin 不可改，只允许调整展示与内容
        if (Boolean.TRUE.equals(existing.getBuiltin())) {
            template.setCode(existing.getCode());
            template.setScene(existing.getScene());
            template.setType(existing.getType());
            template.setBuiltin(true);
            template.setVersion(existing.getVersion());
        } else {
            normalizeAndValidate(template, true);
            template.setCode(AiPromptDefaults.code(template.getType(), template.getScene()));
            Long dup = countBySceneType(template.getScene(), template.getType(), template.getId());
            if (dup != null && dup > 0) {
                throw new CustomException("该场景的" + AiPromptDefaults.typeLabel(template.getType())
                        + "模板已存在，请直接编辑现有模板");
            }
        }
        validatePayload(template);
        // 以现有记录为底更新，避免前端漏传字段被置空
        AiPromptTemplate update = new AiPromptTemplate();
        update.setId(existing.getId());
        update.setCode(template.getCode());
        update.setScene(template.getScene());
        update.setType(template.getType());
        update.setTitle(template.getTitle());
        update.setContent(template.getContent());
        update.setQuickQuestions(template.getQuickQuestions());
        update.setEnabled(template.getEnabled() != null ? template.getEnabled() : existing.getEnabled());
        update.setSort(template.getSort() != null ? template.getSort() : existing.getSort());
        update.setBuiltin(existing.getBuiltin());
        update.setVersion(existing.getVersion());
        this.updateById(update);
        log.info("更新AI提示词模板: code={}, builtin={}", existing.getCode(), existing.getBuiltin());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long id) {
        AiPromptTemplate existing = this.getById(id);
        if (existing == null) {
            throw new CustomException("提示词模板不存在");
        }
        if (Boolean.TRUE.equals(existing.getBuiltin())) {
            throw new CustomException("内置模板不可删除，可使用「重置默认」恢复原始内容");
        }
        this.removeById(id);
        log.info("删除AI提示词模板: code={}", existing.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetBuiltin(Long id) {
        AiPromptTemplate existing = this.getById(id);
        if (existing == null) {
            throw new CustomException("提示词模板不存在");
        }
        if (!Boolean.TRUE.equals(existing.getBuiltin())) {
            throw new CustomException("仅内置模板支持重置默认");
        }
        AiPromptDefaults.TemplateDefault d = AiPromptDefaults.get(existing.getCode());
        if (d == null) {
            throw new CustomException("内置默认内容缺失，无法重置");
        }
        AiPromptTemplate update = new AiPromptTemplate();
        update.setId(id);
        update.setTitle(d.getTitle());
        update.setContent(d.getContent());
        update.setQuickQuestions(d.getQuickQuestions() != null ? JSONUtil.toJsonStr(d.getQuickQuestions()) : null);
        update.setEnabled(true);
        update.setVersion(existing.getVersion());
        this.updateById(update);
        log.info("重置AI提示词模板为默认内容: code={}", existing.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void seedBuiltinsIfMissing() {
        List<AiPromptTemplate> all = this.list();
        Set<String> existingCodes = new HashSet<>();
        for (AiPromptTemplate t : all) {
            existingCodes.add(t.getCode());
        }
        List<AiPromptTemplate> toInsert = new ArrayList<>();
        List<AiPromptDefaults.TemplateDefault> defaults = AiPromptDefaults.all();
        for (int i = 0; i < defaults.size(); i++) {
            AiPromptDefaults.TemplateDefault d = defaults.get(i);
            String code = AiPromptDefaults.code(d.getType(), d.getScene());
            if (existingCodes.contains(code)) {
                continue;
            }
            AiPromptTemplate t = new AiPromptTemplate();
            t.setCode(code);
            t.setScene(d.getScene());
            t.setType(d.getType());
            t.setTitle(d.getTitle());
            t.setContent(d.getContent());
            t.setQuickQuestions(d.getQuickQuestions() != null ? JSONUtil.toJsonStr(d.getQuickQuestions()) : null);
            t.setBuiltin(true);
            t.setEnabled(true);
            // 场景序 × 10 + 类型序（SYSTEM=1/WELCOME=2/QUICK=3）
            t.setSort(resolveSort(d.getScene(), d.getType()));
            t.setVersion(1);
            toInsert.add(t);
        }
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
            log.info("AI提示词内置模板初始化完成，补插 {} 条", toInsert.size());
        }
    }

    /** 取某场景某类型启用中的模板（sort 最小优先）；任何异常静默降级返回 null */
    private AiPromptTemplate findEffective(String scene, String type) {
        if (!AiPromptDefaults.isValidScene(scene)) {
            return null;
        }
        try {
            LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiPromptTemplate::getScene, scene)
                    .eq(AiPromptTemplate::getType, type)
                    .eq(AiPromptTemplate::getEnabled, true)
                    .orderByAsc(AiPromptTemplate::getSort)
                    .last("LIMIT 1");
            return this.getOne(wrapper, false);
        } catch (Exception e) {
            log.warn("读取AI提示词模板失败 scene={} type={}, 降级: {}", scene, type, e.getMessage());
            return null;
        }
    }

    private Long countBySceneType(String scene, String type, Long excludeId) {
        LambdaQueryWrapper<AiPromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPromptTemplate::getScene, scene).eq(AiPromptTemplate::getType, type);
        if (excludeId != null) {
            wrapper.ne(AiPromptTemplate::getId, excludeId);
        }
        return this.count(wrapper);
    }

    /** 新增时基础字段校验（内置更新绕过 scene/type 校验） */
    private void normalizeAndValidate(AiPromptTemplate template, boolean update) {
        if (template == null) {
            throw new CustomException("模板内容不能为空");
        }
        if (template.getScene() == null || !AiPromptDefaults.isValidScene(template.getScene().trim())) {
            throw new CustomException("场景参数非法");
        }
        if (template.getType() == null || !AiPromptDefaults.isValidType(template.getType().trim().toUpperCase())) {
            throw new CustomException("模板类型非法");
        }
        template.setScene(template.getScene().trim());
        template.setType(template.getType().trim().toUpperCase());
        if (template.getTitle() == null || template.getTitle().trim().isEmpty()) {
            throw new CustomException("模板名称不能为空");
        }
        template.setTitle(template.getTitle().trim());
        validatePayload(template);
    }

    /** 按类型校验正文：SYSTEM/WELCOME 用 content，QUICK 用 quickQuestions JSON 数组 */
    private void validatePayload(AiPromptTemplate template) {
        if (AiPromptDefaults.TYPE_QUICK.equals(template.getType())) {
            List<String> questions = parseQuickQuestionsInput(template.getQuickQuestions());
            if (questions.isEmpty()) {
                throw new CustomException("快捷问题至少填写一条");
            }
            // 规范化后回写 JSON，去空白/去空条
            template.setQuickQuestions(JSONUtil.toJsonStr(questions));
            template.setContent(null);
        } else {
            if (template.getContent() == null || template.getContent().trim().isEmpty()) {
                throw new CustomException("模板内容不能为空");
            }
            if (template.getContent().length() > MAX_CONTENT_LENGTH) {
                throw new CustomException("模板内容不能超过" + MAX_CONTENT_LENGTH + "字");
            }
            template.setQuickQuestions(null);
        }
    }

    /**
     * 解析前端提交的快捷问题：兼容 JSON 数组字符串与「每行一条」的纯文本。
     */
    private List<String> parseQuickQuestionsInput(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return result;
        }
        String trimmed = raw.trim();
        List<String> rawList;
        if (trimmed.startsWith("[")) {
            try {
                rawList = JSONUtil.toList(trimmed, String.class);
            } catch (Exception e) {
                throw new CustomException("快捷问题JSON格式不正确");
            }
        } else {
            String[] parts = trimmed.split("\\r?\\n");
            rawList = new ArrayList<>();
            for (String p : parts) {
                rawList.add(p);
            }
        }
        if (rawList == null) {
            return result;
        }
        Set<String> dedup = new HashSet<>();
        for (String q : rawList) {
            if (q == null) {
                continue;
            }
            String item = q.trim();
            if (item.isEmpty() || !dedup.add(item)) {
                continue;
            }
            if (item.length() > MAX_QUESTION_LENGTH) {
                throw new CustomException("快捷问题单条不能超过" + MAX_QUESTION_LENGTH + "字：" + item);
            }
            result.add(item);
        }
        if (result.size() > MAX_QUICK_QUESTIONS) {
            throw new CustomException("快捷问题最多" + MAX_QUICK_QUESTIONS + "条");
        }
        return result;
    }

    private int resolveSort(String scene, String type) {
        int sceneIdx = AiPromptDefaults.SCENES.indexOf(scene);
        int typeIdx;
        if (AiPromptDefaults.TYPE_SYSTEM.equals(type)) {
            typeIdx = 1;
        } else if (AiPromptDefaults.TYPE_WELCOME.equals(type)) {
            typeIdx = 2;
        } else {
            typeIdx = 3;
        }
        return (sceneIdx + 1) * 10 + typeIdx;
    }
}
