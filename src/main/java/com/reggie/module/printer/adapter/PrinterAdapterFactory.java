package com.reggie.module.printer.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 打印机适配器工厂
 * 根据品牌类型返回对应的打印机适配器实例
 *
 * @author reggie
 * @since 2026-07-09
 */
@Component
public class PrinterAdapterFactory {

    /** 佳博打印机适配器 */
    @Autowired
    private GprinterAdapter gprinterAdapter;

    /** 小票打印机适配器 */
    @Autowired
    private XprinterAdapter xprinterAdapter;

    /** Windows系统打印机适配器 */
    @Autowired
    private WindowsSystemPrinterAdapter windowsSystemPrinterAdapter;

    /**
     * 根据品牌类型获取对应的打印机适配器
     *
     * @param brand 品牌类型（GPRINTER/XPRINTER/WINDOWS/SYSTEM）
     * @return 打印机适配器实例
     */
    public PrinterAdapter getAdapter(String brand) {
        if (brand == null) {
            return windowsSystemPrinterAdapter;
        }
        switch (brand.toUpperCase()) {
            case "GPRINTER":
                return gprinterAdapter;
            case "XPRINTER":
                return xprinterAdapter;
            case "WINDOWS":
                return windowsSystemPrinterAdapter;
            case "SYSTEM":
                return windowsSystemPrinterAdapter;
            default:
                return windowsSystemPrinterAdapter;
        }
    }

    /**
     * 获取Windows系统打印机适配器
     *
     * @return Windows系统打印机适配器实例
     */
    public WindowsSystemPrinterAdapter getWindowsAdapter() {
        return windowsSystemPrinterAdapter;
    }
}