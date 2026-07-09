package com.reggie.module.report.service;

import java.util.List;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getDailyReport(String date, Long tenantId);
    List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit, Long tenantId);
    List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate, Long tenantId);
    Map<String, Object> getPaymentAnalysis(String startDate, String endDate, Long tenantId);
    byte[] exportDailyReport(String startDate, String endDate, Long tenantId, String format);

    /**
     * 添加导出记录
     *
     * @param dateRange  日期范围，如 "2026-07-01 ~ 2026-07-08"
     * @param format     导出格式，如 "excel"
     * @param fileName   文件名，如 "report_2026-07-01_2026-07-08.xlsx"
     * @param fileSize   文件大小（字节）
     * @param status     导出状态，如 "success"、"failed"
     */
    void addExportRecord(String dateRange, String format, String fileName, long fileSize, String status);

    /**
     * 获取导出历史记录
     *
     * @return 导出记录列表，按导出时间倒序排列
     */
    List<Map<String, Object>> getExportHistory();

    /**
     * 清除所有导出历史记录
     */
    void clearExportHistory();
}
