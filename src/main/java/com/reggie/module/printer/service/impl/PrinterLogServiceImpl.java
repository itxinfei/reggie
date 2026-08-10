package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.printer.mapper.PrinterLogMapper;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 打印日志服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PrinterLogServiceImpl extends ServiceImpl<PrinterLogMapper, PrinterLog> implements PrinterLogService {

    @Override
    public List<PrinterLog> listByOrderId(Long orderId) {
        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
        return this.list(new LambdaQueryWrapper<PrinterLog>()
                .eq(PrinterLog::getOrderId, orderId)
                .orderByDesc(PrinterLog::getCreatedTime));
    }

    @Override
    public Page<PrinterLog> pageQuery(int page, int pageSize, Long printerId, Integer status) {
        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
        Page<PrinterLog> pageRequest = new Page<>(page, pageSize);
        LambdaQueryWrapper<PrinterLog> wrapper = new LambdaQueryWrapper<PrinterLog>()
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

