package com.reggie.module.billing.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租户订阅记录（每订购/续费一次产生一条，带 tenant_id 受多租户隔离）
 *
 * @author reggie
 * @since 2026-09-12
 */
@Data
@TableName("tenant_subscription")
@Schema(description = "租户SaaS订阅记录")
public class TenantSubscription implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计费周期：月付 */
    public static final int CYCLE_MONTH = 1;
    /** 计费周期：年付 */
    public static final int CYCLE_YEAR = 2;

    /** 状态：待支付 */
    public static final int STATUS_PENDING = 0;
    /** 状态：生效中 */
    public static final int STATUS_ACTIVE = 1;
    /** 状态：已过期 */
    public static final int STATUS_EXPIRED = 2;
    /** 状态：已取消 */
    public static final int STATUS_CANCELED = 3;

    @Schema(description = "订阅记录ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "套餐ID", example = "1")
    private Long planId;

    @Schema(description = "套餐编码快照", example = "STANDARD")
    private String planCode;

    @Schema(description = "套餐名称快照", example = "标准版")
    private String planName;

    @Schema(description = "订阅单号（唯一）", example = "SUB202609120001")
    private String orderNo;

    @Schema(description = "计费周期：1=月付，2=年付", example = "1")
    private Integer billingCycle;

    @Schema(description = "实付金额", example = "299.00")
    private BigDecimal amount;

    @Schema(description = "权益生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "权益到期时间")
    private LocalDateTime endTime;

    @Schema(description = "状态：0=待支付，1=生效中，2=已过期，3=已取消", example = "0")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @Schema(description = "支付渠道：OFFLINE-线下转账，MOCK-模拟开通")
    private String payChannel;

    @Schema(description = "门店数上限快照（-1 不限）", example = "3")
    private Integer maxStores;

    @Schema(description = "员工数上限快照（-1 不限）", example = "20")
    private Integer maxEmployees;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
