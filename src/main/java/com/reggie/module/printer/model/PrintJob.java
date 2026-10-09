package com.reggie.module.printer.model;

import lombok.Data;
import java.util.List;

/**
 * 打印任务信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PrintJob {
    /** 订单ID */
    private Long orderId;

    /** 打印类型（BILL,KITCHEN,DELIVERY） */
    private String printType;

    /** 打印内容行列表 */
    private List<PrintLine> lines;
}
