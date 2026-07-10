package com.reggie.module.printer.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 打印行信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintLine {
    /** 对齐方式枚举 */
    public enum Align { LEFT, CENTER, RIGHT }

    /** 行类型枚举 */
    public enum LineType { TEXT, DIVIDER, QR, BARCODE, TABLE }

    /** 打印文本内容 */
    private String text;

    /** 字体大小 */
    private int fontSize;

    /** 是否加粗 */
    private boolean bold;

    /** 对齐方式 */
    private Align align;

    /** 行类型 */
    private LineType type;
}
