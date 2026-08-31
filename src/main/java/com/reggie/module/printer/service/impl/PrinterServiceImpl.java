package com.reggie.module.printer.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.CustomException;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.mapper.PrintTerminalMapper;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.model.PrintTerminal;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单打印服务实现（门店 PC 本地打印）
 *
 * <p>将订单打印内容入队到门店 PC 打印代理终端（print_task），替代旧的服务器直连打印。
 * 代理端无登录会话，终端/任务数据访问走 {@code @InterceptorIgnore(tenantLine = "true")}
 * 的自定义 Mapper 方法并显式按订单租户匹配。</p>
 *
 * <p>注意：本类<b>禁止</b>类级 @Transactional。历史实现曾用类级事务，导致下单流程中
 * printOrder 抛异常时外层订单事务被标记 rollback-only，整笔下单被打印故障拖垮。
 * 本类方法均为查询 + 单条任务插入，无需强事务。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@Service
public class PrinterServiceImpl implements PrinterService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private PrinterTemplate printerTemplate;

    @Autowired
    private PrintTerminalMapper printTerminalMapper;

    @Autowired
    private PrintTaskMapper printTaskMapper;

    @Override
    public void printOrder(Long orderId, String printType) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }

        List<PrintTerminal> terminals = printTerminalMapper.listEnabledByTenant(order.getTenantId());
        if (terminals.isEmpty()) {
            log.warn("[打印代理] 订单 {} 租户 {} 无启用终端，打印任务未派发", orderId, order.getTenantId());
            return;
        }

        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        PrintJob job = printerTemplate.build(order, details, printType == null ? "BILL" : printType);
        String content = JSONUtil.toJsonStr(job.getLines());

        int dispatched = 0;
        for (PrintTerminal terminal : terminals) {
            if (!matchPrintType(terminal.getPrintTypes(), job.getPrintType())) {
                continue;
            }
            PrintTask task = new PrintTask();
            task.setTenantId(order.getTenantId());
            task.setStoreCode(terminal.getStoreCode());
            task.setOrderId(orderId);
            task.setTaskType(job.getPrintType());
            task.setContent(content);
            task.setStatus(PrintTask.STATUS_PENDING);
            task.setTerminalId(terminal.getId());
            task.setTerminalCode(terminal.getTerminalCode());
            task.setRetryCount(0);
            task.setCreatedTime(LocalDateTime.now());
            printTaskMapper.insertIgnoreTenant(task);
            dispatched++;
        }
        log.info("[打印代理] 订单 {} 派发打印任务 {} 条（type={}）", orderId, dispatched, job.getPrintType());
    }

    /**
     * 终端打印类型匹配：print_types 为空（未配置）视为接收全部类型；
     * 否则逗号分隔精确匹配（如 BILL / KITCHEN / DELIVERY）。
     */
    private boolean matchPrintType(String printTypes, String type) {
        if (printTypes == null || printTypes.trim().isEmpty()) {
            return true;
        }
        for (String s : printTypes.split(",")) {
            if (s.trim().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }
}
