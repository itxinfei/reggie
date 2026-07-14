package com.reggie.module.printer.adapter;

import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;

/**
 * <p>
 * 打印机适配器接口（策略模式），定义与不同品牌票据打印机的交互规范。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public interface PrinterAdapter {
    /**
     * 打印任务
     *
     * @param job    打印任务
     * @param config 打印机配置
     * @return 是否打印成功
     */
    boolean print(PrintJob job, PrinterConfig config);
    /**
     * 查询打印机状态
     *
     * @param config 打印机配置
     * @return 打印机状态
     */
    PrinterStatus queryStatus(PrinterConfig config);
    /**
     * 测试打印机连接
     *
     * @param config 打印机配置
     * @return 是否连接成功
     */
    boolean testConnection(PrinterConfig config);
}
