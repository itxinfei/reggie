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
 * AI聊天图片附件
 * <p>归属（tenant + actor + owner）校验后，模型入参用 base64 data URL 拼装。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Data
@TableName("ai_attachment")
public class AiAttachment {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 上传者身份：EMPLOYEE/CUSTOMER */
    private String actorType;

    /** 上传者用户ID */
    private Long ownerId;

    /** 上传场景 */
    private String scene;

    /** 存储文件名（UUID） */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** MIME类型：image/jpeg、image/png、image/webp */
    private String contentType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 图片宽度（像素） */
    private Integer width;

    /** 图片高度（像素） */
    private Integer height;

    /** 文件内容SHA256 */
    private String sha256;

    /** 相对存储路径：images/ai/{tenantId}/{actorType}/{uuid}.{ext} */
    private String storagePath;

    /** 是否删除 */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    /** 更新人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
