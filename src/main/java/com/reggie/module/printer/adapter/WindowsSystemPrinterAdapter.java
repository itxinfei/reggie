package com.reggie.module.printer.adapter;

import com.reggie.module.printer.model.PrintJob;
import com.reggie.module.printer.model.PrintLine;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.model.PrinterStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * Windows系统打印机适配器，通过Java Print Service API与Windows系统打印机交互。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class WindowsSystemPrinterAdapter implements PrinterAdapter {

    /** 文本MIME类型 */
    private static final String TEXT_MIME_TYPE = "text/plain; charset=UTF-8";

    /**
     * 打印任务
     *
     * @param job    打印任务
     * @param config 打印机配置
     * @return 是否打印成功
     */
    @Override
    public boolean print(PrintJob job, PrinterConfig config) {
        String printerName = getPrinterName(config);
        if (printerName == null || printerName.trim().isEmpty()) {
            log.error("打印失败：打印机名称为空");
            return false;
        }

        try {
            DocPrintJob printJob = getPrintJob(printerName);
            if (printJob == null) {
                log.error("打印失败：找不到打印机: {}", printerName);
                return false;
            }

            byte[] printData = buildPrintData(job);
            InputStream inputStream = new ByteArrayInputStream(printData);

            Doc doc = new SimpleDoc(inputStream, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
            PrintRequestAttributeSet attributes = buildAttributes(config);

            printJob.print(doc, attributes);
            log.info("打印成功：打印机={}, 订单ID={}", printerName, job.getOrderId());
            return true;

        } catch (PrintException e) {
            log.error("打印失败：打印机={}, 错误={}", printerName, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("打印异常：打印机={}, 错误={}", printerName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 查询打印机状态
     *
     * @param config 打印机配置
     * @return 打印机状态
     */
    @Override
    public PrinterStatus queryStatus(PrinterConfig config) {
        PrinterStatus status = new PrinterStatus();
        String printerName = getPrinterName(config);

        if (printerName == null || printerName.trim().isEmpty()) {
            status.setOnline(false);
            status.setDetail("打印机名称为空");
            return status;
        }

        try {
            PrintService service = findPrintService(printerName);
            if (service == null) {
                status.setOnline(false);
                status.setDetail("打印机不存在");
                return status;
            }

            Object state = service.getAttribute(PrinterState.class);
            if (state != null && state.equals(PrinterState.IDLE)) {
                status.setOnline(true);
                status.setDetail("打印机就绪");
            } else {
                status.setOnline(true);
                status.setDetail("打印机状态: " + state);
            }

            return status;

        } catch (Exception e) {
            log.error("查询打印机状态失败: {}", e.getMessage());
            status.setOnline(false);
            status.setDetail("查询失败: " + e.getMessage());
            return status;
        }
    }

    /**
     * 测试打印机连接
     *
     * @param config 打印机配置
     * @return 是否连接成功
     */
    @Override
    public boolean testConnection(PrinterConfig config) {
        String printerName = getPrinterName(config);

        if (printerName == null || printerName.trim().isEmpty()) {
            log.warn("测试失败：打印机名称为空");
            return false;
        }

        try {
            PrintService service = findPrintService(printerName);
            if (service == null) {
                log.warn("测试失败：找不到打印机: {}", printerName);
                return false;
            }

            DocFlavor flavor = DocFlavor.INPUT_STREAM.AUTOSENSE;
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

            if (service.isDocFlavorSupported(flavor)) {
                log.info("测试成功：打印机={} 支持文本打印", printerName);
                return true;
            } else {
                log.warn("测试失败：打印机={} 不支持文本打印", printerName);
                return false;
            }

        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 列出系统所有可用的打印机
     *
     * @return 打印机服务列表
     */
    public List<PrintService> listSystemPrinters() {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(
                DocFlavor.INPUT_STREAM.AUTOSENSE, null);
        return java.util.Arrays.asList(services);
    }

    private String getPrinterName(PrinterConfig config) {
        if (config.getSystemPrinterName() != null && !config.getSystemPrinterName().trim().isEmpty()) {
            return config.getSystemPrinterName();
        }
        return config.getDeviceId();
    }

    private DocPrintJob getPrintJob(String printerName) {
        PrintService service = findPrintService(printerName);
        return service != null ? service.createPrintJob() : null;
    }

    private PrintService findPrintService(String printerName) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(
                DocFlavor.INPUT_STREAM.AUTOSENSE, null);

        for (PrintService service : services) {
            if (service.getName().equalsIgnoreCase(printerName.trim()) ||
                    service.getName().contains(printerName.trim())) {
                return service;
            }
        }
        return null;
    }

    private byte[] buildPrintData(PrintJob job) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<PrintLine> lines = job.getLines();

        try {
            for (PrintLine line : lines) {
                String text = line.getText();

                switch (line.getType()) {
                    case DIVIDER:
                        baos.write("--------------------------------\n".getBytes(StandardCharsets.UTF_8));
                        break;
                    case QR:
                        baos.write(("【二维码】" + text + "\n").getBytes(StandardCharsets.UTF_8));
                        break;
                    case BARCODE:
                        baos.write(("【条形码】" + text + "\n").getBytes(StandardCharsets.UTF_8));
                        break;
                    default:
                        String formattedLine = formatTextLine(line);
                        baos.write(formattedLine.getBytes(StandardCharsets.UTF_8));
                        baos.write("\n".getBytes(StandardCharsets.UTF_8));
                        break;
                }
            }
            baos.write("\n\n\n".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("构建打印数据失败: {}", e.getMessage());
        }

        return baos.toByteArray();
    }

    private String formatTextLine(PrintLine line) {
        String text = line.getText();
        if (text == null) {
            return "";
        }

        int alignment = line.getAlign() != null ? line.getAlign().ordinal() : 0;
        int maxLength = 32;

        if (line.getFontSize() >= 1) {
            text = text + "  ";
        }

        switch (alignment) {
            case 1:
                int padding = Math.max(0, (maxLength - text.length()) / 2);
                return repeatSpace(padding) + text;
            case 2:
                padding = Math.max(0, maxLength - text.length());
                return repeatSpace(padding) + text;
            default:
                return text;
        }
    }

    private String repeatSpace(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private PrintRequestAttributeSet buildAttributes(PrinterConfig config) {
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

        String paperSize = config.getPaperSize();
        if ("80mm".equalsIgnoreCase(paperSize)) {
            attributes.add(OrientationRequested.PORTRAIT);
        } else if ("58mm".equalsIgnoreCase(paperSize)) {
            attributes.add(OrientationRequested.PORTRAIT);
        }

        attributes.add(new Copies(1));
        attributes.add(Sides.ONE_SIDED);

        return attributes;
    }
}