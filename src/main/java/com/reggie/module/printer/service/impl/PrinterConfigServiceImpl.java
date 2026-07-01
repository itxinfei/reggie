package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.printer.mapper.PrinterConfigMapper;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import org.springframework.stereotype.Service;

@Service
public class PrinterConfigServiceImpl extends ServiceImpl<PrinterConfigMapper, PrinterConfig> implements PrinterConfigService {
}
