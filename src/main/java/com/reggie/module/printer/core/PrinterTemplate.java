package com.reggie.module.printer.core;

import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PrinterTemplate {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    public PrintJob delivery(Orders order, List<OrderDetail> details) {
        PrintJob job = new PrintJob();
        job.setOrderId(order.getId());
        job.setPrintType("DELIVERY");

        List<PrintLine> lines = new ArrayList<>();

        lines.add(new PrintLine("=== 配送单 ===", 3, true, PrintLine.Align.CENTER, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("平台: Reggie Takeout", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("订单号: " + order.getNumber(), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));

        for (OrderDetail d : details) {
            String line = d.getName() + " x" + d.getNumber();
            lines.add(new PrintLine(line, 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        }

        lines.add(new PrintLine("", 0, false, PrintLine.Align.LEFT, PrintLine.LineType.DIVIDER));
        lines.add(new PrintLine("配送地址: " + (order.getAddress() != null ? order.getAddress() : ""), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));
        lines.add(new PrintLine("预计送达: " + (order.getCheckoutTime() != null ? order.getCheckoutTime().format(DTF) : ""), 0, false, PrintLine.Align.LEFT, PrintLine.LineType.TEXT));

        job.setLines(lines);
        return job;
    }
}
