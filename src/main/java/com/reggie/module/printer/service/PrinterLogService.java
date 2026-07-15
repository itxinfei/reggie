package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrinterLog;

import java.util.List;

/**
 * <p>
 * 打印日志服务接口
 * </p>
 * <p>记录小票打印的日志信息（打印时间、状态、异常信息等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PrinterLogService extends IService<PrinterLog> {

    /**
     * 根据订单ID查询打印日志
     *
     * @param orderId 订单ID
     * @return 打印日志列表
     */
    List<PrinterLog> listByOrderId(Long orderId);

    /**
     * 分页查询打印日志
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param printerId 打印机ID（可选）
     * @param status   打印状态（可选）
     * @return 分页日志列表
     */
    Page<PrinterLog> pageQuery(int page, int pageSize, Long printerId, Integer status);
}
