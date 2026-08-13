package com.reggie.module.recommend.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 营销活动
 * 定义各类促销活动：满减、折扣、限时优惠等
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("marketing_campaign")
public class MarketingCampaign implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动类型 - 满减 */
    public static final int TYPE_FULL_REDUCTION = 1;
    /** 活动类型 - 折扣 */
    public static final int TYPE_DISCOUNT = 2;
    /** 活动类型 - 赠品 */
    public static final int TYPE_GIFT = 3;
    /** 活动类型 - 首单优惠 */
    public static final int TYPE_FIRST_ORDER = 4;
    /** 活动类型 - 会员专享 */
    public static final int TYPE_MEMBER_EXCLUSIVE = 5;
    /** 活动类型 - 限时秒杀 */
    public static final int TYPE_FLASH_SALE = 6;

    /** 目标类型 - 全部用户 */
    public static final int TARGET_ALL = 1;
    /** 目标类型 - 新用户 */
    public static final int TARGET_NEW_USER = 2;
    /** 目标类型 - 高频用户 */
    public static final int TARGET_HIGH_FREQ = 3;
    /** 目标类型 - 流失预警 */
    public static final int TARGET_CHURN_WARNING = 4;
    /** 目标类型 - 指定等级 */
    public static final int TARGET_SPECIFIC_LEVEL = 5;

    /** 状态 - 草稿 */
    public static final int STATUS_DRAFT = 0;
    /** 状态 - 进行中 */
    public static final int STATUS_ACTIVE = 1;
    /** 状态 - 已结束 */
    public static final int STATUS_ENDED = 2;
    /** 状态 - 已暂停 */
    public static final int STATUS_PAUSED = 3;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 活动名称 */
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称不能超过100个字符")
    private String name;

    /** 活动描述 */
    @Size(max = 500, message = "活动描述不能超过500个字符")
    private String description;

    /** 活动类型 */
    @NotNull(message = "活动类型不能为空")
    private Integer campaignType;

    /** 目标类型 */
    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    /** 目标值 JSON */
    private String targetValue;

    /** 活动规则 JSON */
    private String ruleJson;

    /** 状态 */
    private Integer status;

    /** 优先级 */
    private Integer priority;

    /** 活动开始时间 */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 活动结束时间 */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 最大参与人数 */
    private Integer maxParticipants;

    /** 当前参与人数 */
    private Integer currentParticipants;

    /** 关联优惠券模板ID */
    private Long couponTemplateId;

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

    /**
     * 推送消息数量（不映射数据库字段，通过SQL JOIN计算）
     * 修改点：新增此字段替代前端读取不存在的 pushCount
     */
    @TableField(exist = false)
    private Integer pushCount;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
