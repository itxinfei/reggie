package com.reggie.module.printer.service;

import com.reggie.module.printer.model.PrinterStatus;

/**
 * <p>
 * 打印服务接口
 * </p>
 * <p>提供订单小票打印、打印机测试、状态查询等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PrinterService {

    /**
     * 打印订单小票
     *
     * @param orderId   订单ID
     * @param printType 打印类型（如堂食小票、外卖小票）
     */
    void printOrder(Long orderId, String printType);

    /**
     * 测试打印机连通性
     *
     * @param printerId 打印机ID
     * @return 是否连通
     */
    boolean testPrinter(Long printerId);

    /**
     * 获取打印机状态
     *
     * @param printerId 打印机ID
     * @return 打印机状态信息
     */
    PrinterStatus getPrinterStatus(Long printerId);
}
