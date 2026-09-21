package com.reggie.module.ai.rag.dto;

import lombok.Data;

/**
 * 知识库文档新增/编辑请求（P5）
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
public class KnowledgeDocDTO {

    /** 主键（编辑时必填） */
    private Long id;

    /** 标题 */
    private String title;

    /** 类型：FAQ/TEXT，空值归一为 TEXT */
    private String docType;

    /** 受众：CUSTOMER/MERCHANT/BOTH，空值归一为 BOTH */
    private String audience;

    /** 原文内容 */
    private String content;
}
