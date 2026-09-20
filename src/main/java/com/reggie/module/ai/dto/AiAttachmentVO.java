package com.reggie.module.ai.dto;

import lombok.Data;

/**
 * AI 图片附件上传响应 / 消息内附件描述。
 * <p>存入 ai_message.attachments 的 JSON 元素结构：
 * {@code [{attachmentId,type,mime,url,width,height}]}，前端历史渲染与后端重建共用此契约。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
public class AiAttachmentVO {

    /** 附件ID（ai_attachment 主键），后续发消息以此引用 */
    private Long attachmentId;

    /** 附件类型，当前固定 image */
    private String type;

    /** MIME 类型：image/jpeg、image/png、image/webp */
    private String mime;

    /** 鉴权读取地址（同源，img src 直接可用） */
    private String url;

    /** 宽度（像素，webp 可能为 null） */
    private Integer width;

    /** 高度（像素，webp 可能为 null） */
    private Integer height;

    /** 文件大小（字节） */
    private Long fileSize;
}
