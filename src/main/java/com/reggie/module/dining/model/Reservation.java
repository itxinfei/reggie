package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预订记录信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("dining_reservation")
public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 预订桌台ID */
    private Long tableId;

    /** 客户姓名 */
    private String customerName;

    /** 客户手机号 */
    private String phone;

    /** 预订时间 */
    private LocalDateTime reservedTime;

    /** 预订座位数 */
    private Integer seatCount;

    /** 预订状态：PENDING-待确认，CONFIRMED-已确认，CANCELLED-已取消 */
    private String status;

    /** 备注信息 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
