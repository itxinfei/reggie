package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.printer.mapper.PrinterLogMapper;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import org.springframework.stereotype.Service;

@Service
public class PrinterLogServiceImpl extends ServiceImpl<PrinterLogMapper, PrinterLog> implements PrinterLogService {
}
