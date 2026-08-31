package com.reggie.module.printer.core;

import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 打印模板生成器，根据订单信息生成不同类型的打印任务。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Component
public class PrinterTemplate {

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 根据打印类型构建打印任务
     *
     * @param order     订单信息
     * @param details   订单明细列表
     * @param printType 打印类型（BILL/KITCHEN/DELIVERY）
     * @return 打印任务
     */
    public PrintJob build(Orders order, List<OrderDetail> details, String printType) {
        switch (printType) {
            case "BILL":
                return bill(order, details);
            case "KITCHEN":
                return kitchen(order, details);
            case "DELIVERY":
                return delivery(order, details);
            default:
                return bill(order, details);
        }
    }

    /**
     * 生成收银小票打印任务
     *
     * @param order   订单信息
     * @param details 订单明细列表
     * @return 打印任务
     */
    public PrintJob bill(Orders order, List<OrderDetail> details) {
        PrintJob job = new PrintJob();
        job.setOrderId(order.getId());
        job.setPrintType("BILL");

        List<PrintLine> lines = new ArrayList<>();

        lines.add(new PrintLine("=== 收银小票 ===", 3, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("店铺名称: Reggie Takeout", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("订单号: " + order.getNumber(), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("日期: " + (order.getOrderTime() != null ? order.getOrderTime().format(DTF) : ""), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("--- 菜品明细 ---", 0, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));

        for (OrderDetail d : details) {
            String line = d.getName() + " x" + d.getNumber() + " = " + d.getAmount();
            lines.add(new PrintLine(line, 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }

        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("合计: " + order.getAmount(), 1, true, PrintLine.Align.RIGHT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("请扫码支付", 0, false, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("QR_PLACEHOLDER", 0, false, PrintLine.Align.CENTER, PrintLine.LineType.QR));

        job.setLines(lines);
        return job;
    }

    /**
     * 生成厨房制作单打印任务
     *
     * @param order   订单信息
     * @param details 订单明细列表
     * @return 打印任务
     */
    public PrintJob kitchen(Orders order, List<OrderDetail> details) {
        PrintJob job = new PrintJob();
        job.setOrderId(order.getId());
        job.setPrintType("KITCHEN");

        List<PrintLine> lines = new ArrayList<>();

        lines.add(new PrintLine("=== 厨房制作单 ===", 3, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));
        String tableInfo = order.getTableId() != null ? "桌号: " + order.getTableId() : "";
        lines.add(new PrintLine(tableInfo, 1, true, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("订单号: " + order.getNumber(), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));

        for (OrderDetail d : details) {
            String line = d.getName() + " x" + d.getNumber();
            lines.add(new PrintLine(line, 1, true, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }

        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
            lines.add(new PrintLine("备注: " + order.getRemark(), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }

        job.setLines(lines);
        return job;
    }

    /**
     * 生成外卖单打印任务（堂食配送/平台外卖通用）
     *
     * <p>平台外卖订单（platform_type 非空）额外输出平台名称与平台单号，
     * 便于门店对照平台后台接单；自营配送单仅输出本地单号。</p>
     *
     * @param order   订单信息
     * @param details 订单明细列表
     * @return 打印任务
     */
    public PrintJob delivery(Orders order, List<OrderDetail> details) {
        PrintJob job = new PrintJob();
        job.setOrderId(order.getId());
        job.setPrintType("DELIVERY");

        List<PrintLine> lines = new ArrayList<>();

        lines.add(new PrintLine("=== 外卖单 ===", 3, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));
        boolean isPlatform = StringUtils.isNotBlank(order.getPlatformType());
        lines.add(new PrintLine("平台: " + platformName(order.getPlatformType()), 1, true,
                PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        if (isPlatform && StringUtils.isNotBlank(order.getPlatformOrderId())) {
            lines.add(new PrintLine("平台单号: " + order.getPlatformOrderId(), 0, false,
                    PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }
        lines.add(new PrintLine("订单号: " + order.getNumber(), 0, false,
                PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        if (StringUtils.isNotBlank(order.getUserName())) {
            lines.add(new PrintLine("顾客: " + order.getUserName(), 0, false,
                    PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }
        if (StringUtils.isNotBlank(order.getPhone())) {
            lines.add(new PrintLine("电话: " + order.getPhone(), 0, false,
                    PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }
        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("--- 菜品明细 ---", 0, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));

        for (OrderDetail d : details) {
            String line = d.getName() + " x" + d.getNumber() + " = " + d.getAmount();
            lines.add(new PrintLine(line, 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }

        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("合计: " + order.getAmount(), 1, true,
                PrintLine.Align.RIGHT, PrintLine.LineType.TEXT));
        if (StringUtils.isNotBlank(order.getAddress())) {
            lines.add(new PrintLine("配送地址: " + order.getAddress(), 0, false,
                    PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }
        if (StringUtils.isNotBlank(order.getRemark())) {
            lines.add(new PrintLine("备注: " + order.getRemark(), 0, false,
                    PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }
        lines.add(new PrintLine("下单时间: " + (order.getOrderTime() != null ? order.getOrderTime().format(DTF) : ""),
                0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));

        job.setLines(lines);
        return job;
    }

    /**
     * 平台类型转中文展示名（供小票打印）
     *
     * @param platformType 平台类型（MEITUAN/ELEME/DOUYIN/SELF/OTHER），为空按本店处理
     * @return 中文名称
     */
    private String platformName(String platformType) {
        if (StringUtils.isBlank(platformType)) {
            return "本店";
        }
        switch (platformType.trim().toUpperCase()) {
            case "MEITUAN":
                return "美团";
            case "ELEME":
                return "饿了么";
            case "DOUYIN":
                return "抖音";
            case "SELF":
                return "本店";
            default:
                return platformType;
        }
    }
}

