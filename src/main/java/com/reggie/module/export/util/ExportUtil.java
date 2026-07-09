package com.reggie.module.export.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据导出工具类
 * 提供统一的Excel(.xlsx)和PDF文件生成能力
 * 支持中文编码、自适应列宽、样式美化
 *
 * @author Reggie Team
 */
@Slf4j
public final class ExportUtil {

    private ExportUtil() {
        throw new AssertionError("工具类不允许实例化");
    }

    /** 日期格式化 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== Excel 导出 ====================

    /**
     * 导出Excel文件到Response
     *
     * @param response  HttpServletResponse
     * @param fileName 文件名（不含扩展名）
     * @param columns  列定义: LinkedHashMap<表头, 数据key>
     * @param dataList 数据列表
     */
    public static void exportExcel(HttpServletResponse response, String fileName,
                                   LinkedHashMap<String, String> columns,
                                   List<Map<String, Object>> dataList) {
        try {
            byte[] bytes = generateExcel(columns, dataList);
            setExcelResponse(response, fileName);
            try (OutputStream os = response.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }
            log.info("Excel导出成功: {}, 数据行数: {}", fileName, dataList.size());
        } catch (IOException e) {
            log.error("Excel导出失败: {}", fileName, e);
            throw new RuntimeException("Excel导出失败", e);
        }
    }

    /**
     * 导出Excel文件到字节数组（供Controller缓存或异步处理）
     */
    public static byte[] generateExcelBytes(LinkedHashMap<String, String> columns,
                                            List<Map<String, Object>> dataList) {
        return generateExcel(columns, dataList);
    }

    /**
     * 生成Excel字节数组
     */
    private static byte[] generateExcel(LinkedHashMap<String, String> columns,
                                        List<Map<String, Object>> dataList) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("导出数据");

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            List<String> headerKeys = new ArrayList<>(columns.keySet());
            for (int i = 0; i < headerKeys.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(headerKeys.get(i)));
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);
                Map<String, Object> rowData = dataList.get(rowIdx);
                for (int colIdx = 0; colIdx < headerKeys.size(); colIdx++) {
                    Cell cell = row.createCell(colIdx);
                    Object value = rowData.get(headerKeys.get(colIdx));
                    setCellValue(cell, value);
                    cell.setCellStyle(dataStyle);
                }
            }

            // 自适应列宽
            for (int i = 0; i < headerKeys.size(); i++) {
                sheet.autoSizeColumn(i, true);
                int width = sheet.getColumnWidth(i);
                // 限制最大宽度避免过长
                sheet.setColumnWidth(i, Math.min(width + 2048, 40 * 256));
            }

            // 冻结首行
            sheet.createFreezePane(0, 1);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            log.error("生成Excel失败", e);
            throw new RuntimeException("生成Excel文件失败", e);
        }
    }

    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    /**
     * 创建数据行样式
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setWrapText(true);

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        return style;
    }

    /**
     * 设置单元格值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DATE_FMT));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    // ==================== PDF 导出 ====================

    /**
     * 导出PDF文件到Response
     *
     * @param response  HttpServletResponse
     * @param fileName  文件名（不含扩展名）
     * @param title     文档标题
     * @param columns   列定义
     * @param dataList  数据列表
     * @param summary   汇总信息（可选），如 {"总订单数": "150", "总金额": "¥12,800.00"}
     */
    public static void exportPdf(HttpServletResponse response, String fileName, String title,
                                 LinkedHashMap<String, String> columns,
                                 List<Map<String, Object>> dataList,
                                 Map<String, String> summary) {
        try {
            byte[] bytes = generatePdf(title, columns, dataList, summary);
            setPdfResponse(response, fileName);
            try (OutputStream os = response.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }
            log.info("PDF导出成功: {}, 数据行数: {}", fileName, dataList.size());
        } catch (Exception e) {
            log.error("PDF导出失败: {}", fileName, e);
            throw new RuntimeException("PDF导出失败", e);
        }
    }

    /**
     * 生成PDF字节数组
     */
    public static byte[] generatePdfBytes(String title, LinkedHashMap<String, String> columns,
                                          List<Map<String, Object>> dataList,
                                          Map<String, String> summary) {
        return generatePdf(title, columns, dataList, summary);
    }

    /**
     * 生成PDF
     */
    private static byte[] generatePdf(String title, LinkedHashMap<String, String> columns,
                                      List<Map<String, Object>> dataList,
                                      Map<String, String> summary) {
        try {
            Document document = new Document(PageSize.A4.rotate()); // 横向A4，容纳更多列
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, bos);

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(baseFont, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(baseFont, 10, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(baseFont, 9, com.itextpdf.text.Font.NORMAL);
            com.itextpdf.text.Font summaryFont = new com.itextpdf.text.Font(baseFont, 10, com.itextpdf.text.Font.BOLD);

            document.open();

            // 标题
            Paragraph titlePara = new Paragraph(title, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(8);
            document.add(titlePara);

            // 导出时间
            Paragraph timePara = new Paragraph("导出时间: " + LocalDateTime.now().format(DATE_FMT), dataFont);
            timePara.setAlignment(Element.ALIGN_CENTER);
            timePara.setSpacingAfter(12);
            document.add(timePara);

            // 汇总信息
            if (summary != null && !summary.isEmpty()) {
                for (Map.Entry<String, String> entry : summary.entrySet()) {
                    Paragraph sumPara = new Paragraph(entry.getKey() + ": " + entry.getValue(), summaryFont);
                    sumPara.setSpacingAfter(4);
                    document.add(sumPara);
                }
                document.add(new Paragraph(" "));
            }

            // 创建表格
            List<String> headers = new ArrayList<>(columns.keySet());
            List<String> headerNames = new ArrayList<>(columns.values());
            int colCount = headers.size();
            PdfPTable table = new PdfPTable(colCount);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);

            // 设置列宽（平均分配）
            float[] widths = new float[colCount];
            for (int i = 0; i < colCount; i++) {
                widths[i] = 1f;
            }
            table.setWidths(widths);

            // 表头
            PdfPCell headerCell;
            for (String headerName : headerNames) {
                headerCell = new PdfPCell(new Phrase(headerName, headerFont));
                headerCell.setBackgroundColor(new BaseColor(65, 105, 225)); // 皇家蓝
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerCell.setPadding(5);
                headerCell.setPhrase(new Phrase(headerName,
                        new com.itextpdf.text.Font(baseFont, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE)));
                table.addCell(headerCell);
            }

            // 数据行
            for (int rowIdx = 0; rowIdx < dataList.size(); rowIdx++) {
                Map<String, Object> row = dataList.get(rowIdx);
                for (String header : headers) {
                    Object value = row.get(header);
                    String cellValue = formatCellValue(value);
                    PdfPCell dataCell = new PdfPCell(new Phrase(cellValue, dataFont));
                    dataCell.setPadding(4);
                    dataCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                    // 交替行颜色
                    if (rowIdx % 2 == 1) {
                        dataCell.setBackgroundColor(new BaseColor(245, 245, 250));
                    }
                    table.addCell(dataCell);
                }
            }

            document.add(table);

            // 页脚
            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("— 瑞吉外卖数据报表 —", dataFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new RuntimeException("生成PDF文件失败", e);
        }
    }

    // ==================== 响应头设置 ====================

    /**
     * 设置Excel响应头
     */
    private static void setExcelResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        try {
            String encodedName = URLEncoder.encode(fileName + "_" + LocalDateTime.now().format(FILE_DATE_FMT) + ".xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedName);
        } catch (Exception e) {
            response.setHeader("Content-Disposition", "attachment; filename=export.xlsx");
        }
    }

    /**
     * 设置PDF响应头
     */
    private static void setPdfResponse(HttpServletResponse response, String fileName) {
        response.setContentType("application/pdf");
        response.setCharacterEncoding("UTF-8");
        try {
            String encodedName = URLEncoder.encode(fileName + "_" + LocalDateTime.now().format(FILE_DATE_FMT) + ".pdf", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedName);
        } catch (Exception e) {
            response.setHeader("Content-Disposition", "attachment; filename=export.pdf");
        }
    }

    /**
     * 格式化单元格值
     */
    private static String formatCellValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DATE_FMT);
        }
        return value.toString();
    }
}
