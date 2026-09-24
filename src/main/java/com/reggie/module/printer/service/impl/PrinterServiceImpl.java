package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.printer.core.PrinterTemplate;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单打印服务实现（浏览器本地打印）
 *
 * <p>渲染订单小票纯文本，在 print_task 落一条打印记录（SUCCESS），文本返回前端，
 * 由浏览器调用本地系统打印机打印（window.print），门店无需安装打印代理。</p>
 *
 * <p>注意：本类<b>禁止</b>类级 @Transactional。本方法为查询 + 单条记录插入，无需强事务。</p>
 *
 * @author AI
 * @since 2026-09-21
 */
@Slf4j
@Service
public class PrinterServiceImpl implements PrinterService {

    /** 小票纸宽（按 58mm 热敏纸约 32 个英文字符；80mm 亦可打印） */
    private static final int PAPER_WIDTH = 32;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private PrinterTemplate printerTemplate;

    @Autowired
    private PrintTaskMapper printTaskMapper;

    /**
     * 渲染小票文本并保存打印记录。
     * @param orderId 订单ID
     * @param printType 打印类型 BILL/KITCHEN/DELIVERY
     * @return 小票纯文本
     */
    @Override
    public String renderAndRecord(Long orderId, String printType) {
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        // 校验订单归属当前租户，防止传入他租户 orderId 渲染并输出别家小票内容
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(order.getTenantId())) {
            throw new CustomException("无权操作其他租户的订单");
        }

        List<OrderDetail> details = orderDetailService.list(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderId));
        PrintJob job = printerTemplate.build(order, details, printType == null ? "BILL" : printType);
        String text = linesToText(job.getLines());

        PrintTask task = new PrintTask();
        task.setTenantId(order.getTenantId());
        task.setOrderId(orderId);
        task.setTaskType(job.getPrintType());
        task.setContent(text);
        task.setStatus(PrintTask.STATUS_SUCCESS);
        task.setRetryCount(0);
        LocalDateTime now = LocalDateTime.now();
        task.setCreatedTime(now);
        task.setPulledTime(now);
        task.setDoneTime(now);
        printTaskMapper.insertIgnoreTenant(task);

        log.info("[浏览器打印] 订单 {} 已生成打印记录（type={}）", orderId, job.getPrintType());
        return text;
    }

    /**
     * 将结构化打印行转为等宽纯文本小票。
     * 分隔线输出整行 '-'；居中/右对齐按显示宽度（中文计 2）补空格；
     * 二维码/条形码行在浏览器纸质小票中无意义，跳过。
     */
    private String linesToText(List<PrintLine> lines) {
        StringBuilder sb = new StringBuilder();
        for (PrintLine line : lines) {
            String text = line.getText() == null ? "" : line.getText();
            if (line.getType() == PrintLine.LineType.QR || line.getType() == PrintLine.LineType.BARCODE) {
                continue;
            }
            if (line.getType() == PrintLine.LineType.DIVIDER) {
                appendLine(sb, repeat('-', PAPER_WIDTH));
                continue;
            }
            int width = displayWidth(text);
            if (line.getAlign() == PrintLine.Align.CENTER) {
                int pad = (PAPER_WIDTH - width) / 2;
                appendLine(sb, repeat(' ', Math.max(pad, 0)) + text);
            } else if (line.getAlign() == PrintLine.Align.RIGHT) {
                int pad = PAPER_WIDTH - width;
                appendLine(sb, repeat(' ', Math.max(pad, 0)) + text);
            } else {
                appendLine(sb, text);
            }
        }
        return sb.toString();
    }

    /** 追加一行（首行不加前置换行） */
    private void appendLine(StringBuilder sb, String line) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(line);
    }

    /** 重复字符（JDK1.8 无 String.repeat） */
    private String repeat(char c, int n) {
        if (n <= 0) {
            return "";
        }
        char[] arr = new char[n];
        for (int i = 0; i < n; i++) {
            arr[i] = c;
        }
        return new String(arr);
    }

    /** 计算显示宽度：CJK 等宽字符计 2，其余计 1 */
    private int displayWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isWide(ch)) {
                w += 2;
            } else {
                w += 1;
            }
        }
        return w;
    }

    /** 是否为全角宽字符（CJK 统一表意文字/全角标点/平假名/片假名等） */
    private boolean isWide(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA;
    }
}
