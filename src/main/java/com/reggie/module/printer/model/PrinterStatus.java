package com.reggie.module.printer.model;

import lombok.Data;

/**
 * 打印机状态信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PrinterStatus {
    /** 是否在线 */
    private boolean online;

    /** 状态详情描述 */
    private String detail;
}
