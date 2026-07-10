package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.printer.mapper.PrinterConfigMapper;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import org.springframework.stereotype.Service;

/**
 * 打印机配置服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class PrinterConfigServiceImpl extends ServiceImpl<PrinterConfigMapper, PrinterConfig> implements PrinterConfigService {
}
