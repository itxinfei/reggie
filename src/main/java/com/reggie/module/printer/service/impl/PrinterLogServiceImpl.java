package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.printer.mapper.PrinterLogMapper;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 打印日志服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
public class PrinterLogServiceImpl extends ServiceImpl<PrinterLogMapper, PrinterLog> implements PrinterLogService {

    @Override
    public List<PrinterLog> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<PrinterLog>()
                .eq(PrinterLog::getOrderId, orderId)
                .eq(PrinterLog::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(PrinterLog::getCreatedTime));
    }

    @Override
    public Page<PrinterLog> pageQuery(int page, int pageSize, Long printerId, Integer status) {
        Page<PrinterLog> pageRequest = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> wrapper = new LambdaQueryWrapper<PrinterLog>()
                .eq(PrinterLog::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(PrinterLog::getCreatedTime);
        if (printerId != null) {
            wrapper.eq(PrinterLog::getPrinterId, printerId);
        }
        if (status != null) {
            wrapper.eq(PrinterLog::getStatus, status);
        }
        return this.page(pageRequest, wrapper);
    }
}
