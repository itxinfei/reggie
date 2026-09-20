package com.reggie.module.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.ai.constant.AiPromptDefaults;
import com.reggie.module.ai.model.AiPromptTemplate;
import com.reggie.module.ai.service.AiPromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 提示词模板管理控制器（后台管理，P3）
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai/prompt")
@RequireEmployee
@Tag(name = "AI提示词模板", description = "管理各场景系统提示词、欢迎语与快捷问题")
public class AiPromptTemplateController {

    @Resource
    private AiPromptTemplateService promptTemplateService;

    /**
     * 分页查询。
     * @param page 页码
     * @param pageSize 每页条数
     * @param scene 场景
     * @param type 类型
     * @param keyword 标题/编码关键词
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "模板分页", description = "按场景/类型/关键词分页查询提示词模板")
    public R<Page<AiPromptTemplate>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "场景") @RequestParam(required = false) String scene,
            @Parameter(description = "类型") @RequestParam(required = false) String type,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }
        return R.success(promptTemplateService.adminPage(page, pageSize, scene, type, keyword));
    }

    /**
     * 场景与类型字典（前端下拉用）。
     * @return 字典
     */
    @GetMapping("/meta")
    @Operation(summary = "场景类型字典", description = "返回合法场景与模板类型清单")
    public R<Map<String, Object>> meta() {
        List<Map<String, String>> scenes = new ArrayList<>();
        for (String scene : AiPromptDefaults.SCENES) {
            Map<String, String> item = new HashMap<>();
            item.put("value", scene);
            item.put("label", AiPromptDefaults.sceneLabel(scene));
            scenes.add(item);
        }
        List<Map<String, String>> types = new ArrayList<>();
        for (String type : AiPromptDefaults.TYPES) {
            Map<String, String> item = new HashMap<>();
            item.put("value", type);
            item.put("label", AiPromptDefaults.typeLabel(type));
            types.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("scenes", scenes);
        data.put("types", types);
        return R.success(data);
    }

    /**
     * 新增自定义模板。
     * @param template 模板
     * @return 新ID
     */
    @PostMapping("/add")
    @Operation(summary = "新增模板", description = "新增自定义提示词模板，同场景同类型仅允许一条")
    public R<Map<String, Object>> add(
            @Parameter(description = "模板内容", required = true) @RequestBody AiPromptTemplate template) {
        Long id = promptTemplateService.createTemplate(template);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return R.success(result);
    }

    /**
     * 更新模板（内置模板仅可改标题/内容/启用状态/排序）。
     * @param template 模板
     * @return 结果
     */
    @PostMapping("/update")
    @Operation(summary = "更新模板", description = "修改提示词模板，内置模板的场景/类型/编码不可改")
    public R<String> update(
            @Parameter(description = "模板内容", required = true) @RequestBody AiPromptTemplate template) {
        promptTemplateService.updateTemplate(template);
        return R.success("更新成功");
    }

    /**
     * 删除自定义模板（内置拒绝）。
     * @param id 模板ID
     * @return 结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模板", description = "删除自定义模板，内置模板不可删除")
    public R<String> delete(@PathVariable Long id) {
        promptTemplateService.deleteTemplate(id);
        return R.success("删除成功");
    }

    /**
     * 内置模板重置默认。
     * @param id 模板ID
     * @return 结果
     */
    @PostMapping("/reset/{id}")
    @Operation(summary = "重置默认", description = "将内置模板内容恢复为系统默认")
    public R<String> reset(@PathVariable Long id) {
        promptTemplateService.resetBuiltin(id);
        return R.success("已重置为默认内容");
    }
}
