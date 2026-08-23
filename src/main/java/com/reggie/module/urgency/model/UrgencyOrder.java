package com.reggie.module.urgency.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 催单订单信息类（Mock 数据用，未建表）
 *
 * @author reggie
 * @since 2026-08-23
 */
@Data
public class UrgencyOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 桌号 */
    private String tableNo;

    /** 顾客姓名 */
    private String customerName;

    /** 菜品名列表（逗号分隔） */
    private String dishNames;

    /** 订单状态：COOKING-制作中, WAITING_CALL-等待叫号, COMPLETED-已完成 */
    private String status;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 预估完成时间 */
    private LocalDateTime estimatedFinishTime;

    /** 制作进度百分比（0-100） */
    private Integer progressPercent;

    /** 租户ID */
    private Long tenantId;
}
