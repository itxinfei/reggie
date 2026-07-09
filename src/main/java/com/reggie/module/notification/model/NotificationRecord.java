package com.reggie.module.notification.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知发送记录实体
 * 记录每次短信/APP推送的发送详情与结果
 */
@Data
@TableName("notification_record")
public class NotificationRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联模板ID */
    private Long templateId;

    /** 业务类型 */
    private String bizType;

    /** 发送渠道: 1=短信, 2=APP推送 */
    private Integer channel;

    /** 目标类型: 1=单个用户, 2=用户分组, 3=全部用户 */
    private Integer targetType;

    /** 目标值(JSON数组: 手机号/用户ID列表) */
    private String targetValue;

    /** 目标数量 */
    private Integer targetCount;

    /** 实际发送内容 */
    private String content;

    /** 定时发送时间，NULL表示立即发送 */
    private LocalDateTime sendTime;

    /** 状态: 0=待发送, 1=发送中, 2=成功, 3=失败, 4=部分成功 */
    private Integer status;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 失败原因汇总 */
    private String failReason;

    /** 扩展数据JSON */
    private String extData;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;
}
