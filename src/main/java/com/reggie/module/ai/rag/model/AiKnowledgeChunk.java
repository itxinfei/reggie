package com.reggie.module.ai.rag.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库切块（P5 RAG）
 * <p>文档按 400 字/50 字重叠切分的最小检索单元；embedding 为向量 JSON（无 embedding 模型时为 NULL，
 * 检索自动降级 ngram FULLTEXT → LIKE）。重建索引时整组物理删除，不做逻辑删除。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 所属文档ID */
    private Long docId;

    /** 文档内序号（从 0 开始） */
    private Integer chunkIndex;

    /** 切块文本（≤400 字） */
    private String content;

    /** 向量 JSON（double 数组字符串），无向量时为 NULL */
    private String embedding;

    /** 生成向量所用模型名（模型切换后旧向量不参与余弦，需重建索引） */
    private String embedModel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
