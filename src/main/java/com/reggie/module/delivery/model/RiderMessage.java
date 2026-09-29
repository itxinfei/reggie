package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 骑手消息：骑手个人收件箱条目，由派单 / 催单 / 异常处理 / 系统公告等场景写入。
 *
 * <p>类型枚举：{@code 1 派单提醒, 2 催单提醒, 3 异常处理, 4 系统公告, 5 其他}；
 * 已读：{@code 0 未读, 1 已读}。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_message")
@Schema(description = "骑手消息")
public class RiderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 派单提醒 */
    public static final int TYPE_DISPATCH = 1;
    /** 催单提醒 */
    public static final int TYPE_URGENCY = 2;
    /** 异常处理结果 */
    public static final int TYPE_EXCEPTION = 3;
    /** 系统公告 */
    public static final int TYPE_NOTICE = 4;
    /** 其他 */
    public static final int TYPE_OTHER = 5;

    /** 未读 */
    public static final int UNREAD = 0;
    /** 已读 */
    public static final int READ = 1;

    @Schema(description = "消息ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "接收骑手ID")
    private Long riderId;

    @Schema(description = "消息类型：1派单提醒 2催单提醒 3异常处理 4系统公告 5其他")
    private Integer type;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "关联业务ID（订单ID）")
    private Long bizId;

    @Schema(description = "是否已读：0未读 1已读")
    private Integer isRead;

    @Schema(description = "阅读时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableLogic
    private Integer isDeleted;
}
