package com.reggie.module.printer.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrintLine {
    public enum Align { LEFT, CENTER, RIGHT }
    public enum LineType { TEXT, DIVIDER, QR, BARCODE, TABLE }

    private String text;
    private int fontSize;
    private boolean bold;
    private Align align;
    private LineType type;
}
