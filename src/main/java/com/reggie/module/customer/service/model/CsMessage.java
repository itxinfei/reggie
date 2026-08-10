package com.reggie.module.customer.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Customer Service Message Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("cs_message")
@Schema(description = "Customer Service Message")
public class CsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Sender Type - User */
    public static final int SENDER_USER = 1;
    /** Sender Type - Agent */
    public static final int SENDER_AGENT = 2;
    /** Sender Type - System */
    public static final int SENDER_SYSTEM = 3;

    /** Message Type - Text */
    public static final int TYPE_TEXT = 1;
    /** Message Type - Image */
    public static final int TYPE_IMAGE = 2;
    /** Message Type - Order Card */
    public static final int TYPE_ORDER_CARD = 3;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Session ID")
    private Long sessionId;

    @Schema(description = "Sender Type: 1-User, 2-Agent, 3-System")
    private Integer senderType;

    @Schema(description = "Sender ID")
    private Long senderId;

    @Schema(description = "Sender Name")
    private String senderName;

    @Schema(description = "Message Type: 1-Text, 2-Image, 3-Order Card")
    private Integer messageType;

    @Schema(description = "Message Content")
    private String content;

    @Schema(description = "Image URL")
    private String imageUrl;

    @Schema(description = "Is Read: 0-No, 1-Yes")
    private Integer isRead;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;
}
