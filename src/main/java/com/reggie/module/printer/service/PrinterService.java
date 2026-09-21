package com.reggie.module.printer.service;

/**
 * 订单打印服务
 *
 * <p>浏览器本地打印场景：渲染订单小票纯文本，在 {@code print_task} 落一条打印记录（SUCCESS），
 * 文本返回前端，由浏览器调用本地系统打印机打印（window.print）。门店无需安装打印代理。</p>
 *
 * @author AI
 * @since 2026-09-21
 */
public interface PrinterService {

    /**
     * 渲染订单小票文本并保存一条打印记录。
     *
     * @param orderId   订单ID
     * @param printType 打印类型：BILL-收银小票、KITCHEN-厨房制作单、DELIVERY-配送单
     * @return 小票纯文本，供前端浏览器打印
     */
    String renderAndRecord(Long orderId, String printType);
}
