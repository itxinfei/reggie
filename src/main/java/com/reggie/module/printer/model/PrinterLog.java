package com.reggie.module.printer.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印日志信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("printer_log")
public class PrinterLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 订单ID */
    private Long orderId;

    /** 打印类型（BILL,KITCHEN,DELIVERY） */
    private String printType;

    /** 打印机ID */
    private Long printerId;

    /** 打印内容 */
    private String content;

    /** 打印状态：0-失败，1-成功 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
