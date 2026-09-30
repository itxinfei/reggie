package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 提示词模板历史版本快照（P5 运营闭环）
 * <p>模板每次更新/重置/回滚前自动把当前内容写入本表，支持运营回滚与变更追溯；
 * 与 {@link AiPromptTemplate} 同构但无 tenant_id（系统级表，已加入租户插件白名单）。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
@Data
@TableName("ai_prompt_template_history")
public class AiPromptTemplateHistory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属模板ID（ai_prompt_template.id） */
    private Long templateId;

    /** 模板编码（快照时点） */
    private String code;

    /** 场景（快照时点） */
    private String scene;

    /** 类型 SYSTEM/WELCOME/QUICK（快照时点） */
    private String type;

    /** 模板名称（快照时点） */
    private String title;

    /** 正文内容（快照时点） */
    private String content;

    /** 快捷问题JSON（快照时点） */
    private String quickQuestions;

    /** 是否启用（快照时点） */
    private Boolean enabled;

    /** 该快照对应的模板版本号 */
    private Integer version;

    /** 触发快照的操作人（员工ID） */
    private Long operatorId;

    /** 快照时间 */
    private LocalDateTime createTime;
}
