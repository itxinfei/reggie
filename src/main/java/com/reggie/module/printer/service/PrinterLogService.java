package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrinterLog;

/**
 * 打印日志服务接口
 * 记录小票打印的日志信息（打印时间、状态、异常信息等）
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface PrinterLogService extends IService<PrinterLog> {
}
