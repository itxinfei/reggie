package com.reggie.module.notification.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知模板实体
 * 支持短信和APP推送两种渠道的消息模板管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("notification_template")
public class NotificationTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 模板名称 */
    private String templateName;

    /** 外部模板编码(阿里云SMS模板CODE等) */
    private String templateCode;

    /** 通知渠道: 1=短信, 2=APP推送, 3=短信+APP推送 */
    private Integer channel;

    /** 业务类型: ORDER_NOTICE/PROMOTION/VERIFY_CODE/SYSTEM/JOB_NOTICE */
    private String bizType;

    /** 推送标题(APP推送使用) */
    private String title;

    /** 模板内容，支持占位符 ${param} */
    private String content;

    /** 参数列表JSON */
    private String paramList;

    /** 短信签名 */
    private String signName;

    /** 状态: 1=启用, 0=停用 */
    private Integer status;

    /** 备注说明 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
