package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.printer.mapper.PrinterLogMapper;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import org.springframework.stereotype.Service;

/**
 * 打印日志服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class PrinterLogServiceImpl extends ServiceImpl<PrinterLogMapper, PrinterLog> implements PrinterLogService {
}
