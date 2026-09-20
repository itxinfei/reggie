package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 提示词模板（P3）
 * <p>承载各场景的系统提示词（SYSTEM）、欢迎语（WELCOME）、快捷问题（QUICK），
 * 内置模板 builtin=1 不可删、可重置默认；系统级表（同 ai_provider_config，无 tenant_id）。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Data
@TableName("ai_prompt_template")
public class AiPromptTemplate {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板编码：{type 小写}_{scene}，如 system_order_assistant */
    private String code;

    /** 场景：order_assistant/dish_desc/business_analysis/marketing */
    private String scene;

    /** 类型：SYSTEM/WELCOME/QUICK */
    private String type;

    /** 模板名称（后台展示） */
    private String title;

    /** 提示词/欢迎语文本（SYSTEM/WELCOME 使用） */
    private String content;

    /** 快捷问题 JSON 数组字符串（QUICK 使用） */
    private String quickQuestions;

    /** 是否内置：内置不可删，可重置默认 */
    private Boolean builtin;

    /** 是否启用 */
    private Boolean enabled;

    /** 排序号 */
    private Integer sort;

    /** 模板版本（内置默认内容版本） */
    private Integer version;

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
