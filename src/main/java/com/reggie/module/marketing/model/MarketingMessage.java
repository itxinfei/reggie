package com.reggie.module.marketing.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销消息推送记录
 * 记录每次营销消息的推送、阅读和使用情况
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("marketing_message")
public class MarketingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 推送类型 - 首页弹窗 */
    public static final int PUSH_POPUP = 1;
    /** 推送类型 - 消息通知 */
    public static final int PUSH_NOTIFICATION = 2;
    /** 推送类型 - 短信 */
    public static final int PUSH_SMS = 3;
    /** 推送类型 - 优惠券自动发放 */
    public static final int PUSH_COUPON = 4;

    /** 状态 - 待推送 */
    public static final int STATUS_PENDING = 0;
    /** 状态 - 已推送 */
    public static final int STATUS_SENT = 1;
    /** 状态 - 已读 */
    public static final int STATUS_READ = 2;
    /** 状态 - 已使用 */
    public static final int STATUS_USED = 3;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 关联营销活动ID */
    private Long campaignId;

    /** 推送用户ID */
    private Long userId;

    /** 推送类型 */
    private Integer pushType;

    /** 推送标题 */
    private String title;

    /** 推送内容 */
    private String content;

    /** 状态 0:待推送 1:已推送 2:已读 3:已使用 */
    private Integer status;

    /** 阅读时间 */
    private LocalDateTime readTime;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}

