package com.reggie.module.ai.rag.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.ai.rag.dto.KnowledgeDocDTO;
import com.reggie.module.ai.rag.model.AiKnowledgeDoc;
import com.reggie.module.ai.rag.service.AiKnowledgeService;
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
import java.util.HashMap;
import java.util.Map;

/**
 * AI 知识库管理控制器（后台管理，P5 RAG）。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai/knowledge")
@RequireEmployee
@Tag(name = "AI知识库", description = "管理商家知识库文档、索引重建与状态监控")
public class KnowledgeController {

    @Resource
    private AiKnowledgeService knowledgeService;

    /**
     * 分页查询。
     * @param page 页码
     * @param pageSize 每页条数
     * @param keyword 标题关键词
     * @param audience 受众
     * @param status 状态
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "文档分页", description = "按标题/受众/状态分页查询知识库文档")
    public R<IPage<AiKnowledgeDoc>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "标题关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "受众") @RequestParam(required = false) String audience,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }
        return R.success(knowledgeService.adminPage(page, pageSize, keyword, audience, status));
    }

    /**
     * 状态统计。
     * @return total/ready/indexing/failed
     */
    @GetMapping("/stats")
    @Operation(summary = "状态统计", description = "返回文档总数与各索引状态数量")
    public R<Map<String, Object>> stats() {
        return R.success(knowledgeService.stats());
    }

    /**
     * 新增文档（保存后自动异步索引）。
     * @param dto 文档内容
     * @return 新ID
     */
    @PostMapping("/add")
    @Operation(summary = "新增文档", description = "新增知识库文档并触发异步索引")
    public R<Map<String, Object>> add(
            @Parameter(description = "文档内容", required = true) @RequestBody KnowledgeDocDTO dto) {
        Long id = knowledgeService.createDoc(dto);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        return R.success(result);
    }

    /**
     * 更新文档（原文变更自动重新索引）。
     * @param dto 文档内容
     * @return 结果
     */
    @PostMapping("/update")
    @Operation(summary = "更新文档", description = "修改知识库文档，原文变更时重新索引")
    public R<String> update(
            @Parameter(description = "文档内容", required = true) @RequestBody KnowledgeDocDTO dto) {
        knowledgeService.updateDoc(dto);
        return R.success("更新成功");
    }

    /**
     * 删除文档。
     * @param id 文档ID
     * @return 结果
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除文档", description = "删除文档及其全部切块")
    public R<String> delete(@PathVariable Long id) {
        knowledgeService.deleteDoc(id);
        return R.success("删除成功");
    }

    /**
     * 手动重建索引。
     * @param id 文档ID
     * @return 结果
     */
    @PostMapping("/reindex/{id}")
    @Operation(summary = "重建索引", description = "手动触发文档切块与向量重建")
    public R<String> reindex(@PathVariable Long id) {
        knowledgeService.reindex(id);
        return R.success("已提交索引任务");
    }
}
