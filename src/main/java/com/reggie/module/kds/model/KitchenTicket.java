package com.reggie.module.kds.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后厨厨房工单（KDS）。订单支付/下单后由本模块幂等拉取生成，
 * 独立于订单配送状态机流转：待制作 → 制作中 → 待取餐（叫号）→ 已完成，可取消。
 *
 * @author reggie
 * @since 2026-09-13
 */
@Data
@TableName("kitchen_ticket")
@Schema(description = "后厨厨房工单")
public class KitchenTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：待制作 */
    public static final int STATUS_PENDING = 1;
    /** 状态：制作中 */
    public static final int STATUS_COOKING = 2;
    /** 状态：待取餐（已叫号） */
    public static final int STATUS_READY = 3;
    /** 状态：已完成（已取餐/已出餐） */
    public static final int STATUS_FINISHED = 4;
    /** 状态：已取消 */
    public static final int STATUS_CANCELLED = 5;

    /** 非加急 */
    public static final int URGENT_NO = 0;
    /** 加急 */
    public static final int URGENT_YES = 1;

    @Schema(description = "工单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "来源订单ID（唯一，幂等拉取）")
    private Long orderId;

    @Schema(description = "取餐号/订单号快照（orders.number）")
    private String orderNo;

    @Schema(description = "用餐类型快照（orders.source/dining_type，如 EAT_IN 堂食、OUTSIDE 外卖）")
    private String orderType;

    @Schema(description = "堂食桌台名称")
    private String tableName;

    @Schema(description = "用餐人数")
    private Integer customerCount;

    @Schema(description = "工单状态：1待制作，2制作中，3待取餐，4已完成，5已取消")
    private Integer status;

    @Schema(description = "是否加急：0否，1是")
    private Integer urgent;

    @Schema(description = "菜品摘要快照，如：鱼香肉丝x2；米饭x1")
    private String dishSummary;

    @Schema(description = "接单/生成时间（等待时长起点）")
    private LocalDateTime receiveTime;

    @Schema(description = "开始制作时间")
    private LocalDateTime cookStartTime;

    @Schema(description = "制作完成/叫号时间")
    private LocalDateTime readyTime;

    @Schema(description = "取餐完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    @Schema(description = "制作耗时（秒）：开始制作到叫号")
    private Long cookDurationSeconds;

    @Schema(description = "档口编码（如 HOT=热菜, COLD=凉菜, DRINK=饮品, DESSERT=甜点）")
    private String stationCode;

    @Schema(description = "备注（订单备注快照）")
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
