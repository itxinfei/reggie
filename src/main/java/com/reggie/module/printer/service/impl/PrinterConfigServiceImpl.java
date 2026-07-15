package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.printer.mapper.PrinterConfigMapper;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 打印机配置服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
public class PrinterConfigServiceImpl extends ServiceImpl<PrinterConfigMapper, PrinterConfig> implements PrinterConfigService {

    @Override
    public List<PrinterConfig> listByTenant() {
        return this.list(new LambdaQueryWrapper<PrinterConfig>()
                .eq(PrinterConfig::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(PrinterConfig::getSort));
    }

    @Override
    public PrinterConfig getByType(String printerType) {
        return this.list(new LambdaQueryWrapper<PrinterConfig>()
                .eq(PrinterConfig::getType, printerType)
                .eq(PrinterConfig::getTenantId, BaseContext.getCurrentTenantId())
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }
}
