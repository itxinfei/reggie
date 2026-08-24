package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
    @TableField(fill = FieldFill.INSERT)
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
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
