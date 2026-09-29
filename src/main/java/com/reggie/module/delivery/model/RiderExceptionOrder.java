package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 骑手异常工单：配送过程中骑手上报的异常（联系不上顾客 / 商品破损 / 地址有误 /
 * 顾客拒收 / 申请转单 / 其他），由后台处理（解决 / 关闭 / 改派）。
 *
 * <p>状态枚举：{@code 0 待处理, 1 已处理, 2 已转单, 3 已关闭}；
 * 异常类型：{@code 1 联系不上顾客, 2 商品破损, 3 地址有误, 4 顾客拒收, 5 申请转单, 6 其他}。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Data
@TableName("rider_exception_order")
@Schema(description = "骑手异常工单")
public class RiderExceptionOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理 */
    public static final int STATUS_PENDING = 0;
    /** 已处理 */
    public static final int STATUS_HANDLED = 1;
    /** 已转单/改派 */
    public static final int STATUS_TRANSFERRED = 2;
    /** 已关闭 */
    public static final int STATUS_CLOSED = 3;

    /** 联系不上顾客 */
    public static final int TYPE_UNREACHABLE = 1;
    /** 商品破损/洒漏 */
    public static final int TYPE_DAMAGED = 2;
    /** 地址有误 */
    public static final int TYPE_ADDRESS = 3;
    /** 顾客拒收 */
    public static final int TYPE_REJECTED = 4;
    /** 申请转单 */
    public static final int TYPE_TRANSFER = 5;
    /** 其他 */
    public static final int TYPE_OTHER = 6;

    @Schema(description = "工单ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNumber;

    @Schema(description = "上报骑手ID")
    private Long riderId;

    @Schema(description = "上报骑手姓名")
    private String riderName;

    @Schema(description = "异常类型：1联系不上顾客 2商品破损 3地址有误 4顾客拒收 5申请转单 6其他")
    private Integer exceptionType;

    @Schema(description = "异常描述")
    private String description;

    @Schema(description = "状态：0待处理 1已处理 2已转单 3已关闭")
    private Integer status;

    @Schema(description = "处理备注")
    private String handleNote;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "处理时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

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
