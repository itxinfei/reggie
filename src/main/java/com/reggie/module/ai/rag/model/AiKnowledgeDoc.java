package com.reggie.module.ai.rag.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库文档（P5 RAG）
 * <p>商家维护的知识条目（FAQ/长文本），保存后异步切块索引；
 * 按受众 audience 控制注入范围（C 端点餐场景仅见 CUSTOMER/BOTH，后台全场景见 MERCHANT/BOTH）。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
@TableName("ai_knowledge_doc")
public class AiKnowledgeDoc {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID（插入时自动填充，查询经租户插件自动过滤） */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 标题 */
    private String title;

    /** 类型：FAQ/TEXT */
    private String docType;

    /** 受众：CUSTOMER/MERCHANT/BOTH */
    private String audience;

    /** 原文内容（应用层限制 ≤20000 字） */
    private String content;

    /** 索引状态：DRAFT/INDEXING/READY/FAILED */
    private String status;

    /** 切块数量 */
    private Integer chunkCount;

    /** 失败/降级原因（embedding 不可用时的降级提示也记录于此） */
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
