package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用餐桌台信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class DiningTable implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 区域ID */
    private Long areaId;

    /** 区域名称（非数据库字段，用于关联查询） */
    @TableField(exist = false)
    private String areaName;

    /** 桌台名称/编号 */
    private String name;

    /** 座位数 */
    private Integer seatCount;

    /** 桌台状态：FREE-空闲，OCCUPIED-使用中，RESERVED-已预订 */
    private String status;

    /** 最低消费金额 */
    private BigDecimal minAmount;

    /** 桌台二维码URL */
    private String qrCodeUrl;

    /** 排序号 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
