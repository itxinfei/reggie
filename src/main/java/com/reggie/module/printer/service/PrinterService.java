package com.reggie.module.printer.service;

/**
 * 订单打印服务
 *
 * <p>门店 PC 本地打印场景：将订单打印内容构建为 {@code PrintLine} JSON 后
 * 入队 {@code print_task}（PENDING），由门店 PC 上的打印代理心跳领取并调用本地打印机。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
public interface PrinterService {

    /**
     * 派发订单打印任务：按订单租户匹配门店启用终端，为每个接收该打印类型的终端入队一条任务。
     * 无匹配终端时仅记日志，不影响调用方（订单/收银流程）。
     *
     * @param orderId   订单ID
     * @param printType 打印类型：BILL-收银小票、KITCHEN-厨房制作单、DELIVERY-配送单
     */
    void printOrder(Long orderId, String printType);
}
