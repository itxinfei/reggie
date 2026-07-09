package com.reggie.module.store.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店每日经营汇总
 * 总部控制台聚合各门店经营数据的快照表
 */
@Data
@TableName("store_daily_summary")
public class StoreDailySummary implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 门店ID */
    private Long tenantId;

    /** 统计日期 */
    private LocalDate summaryDate;

    /** 订单总数 */
    private Integer totalOrders;

    /** 已完成订单数 */
    private Integer completedOrders;

    /** 取消订单数 */
    private Integer cancelledOrders;

    /** 订单总额 */
    private BigDecimal totalAmount;

    /** 实收金额 */
    private BigDecimal actualAmount;

    /** 新增用户数 */
    private Integer newUsers;

    /** 平均订单金额 */
    private BigDecimal avgOrderAmount;

    /** 热销菜品TOP10 JSON */
    private String topDishJson;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
