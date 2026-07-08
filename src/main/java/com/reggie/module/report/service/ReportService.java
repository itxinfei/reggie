package com.reggie.module.report.service;

import java.util.List;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getDailyReport(String date, Long tenantId);
    List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit, Long tenantId);
    List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate, Long tenantId);
    Map<String, Object> getPaymentAnalysis(String startDate, String endDate, Long tenantId);
    byte[] exportDailyReport(String startDate, String endDate, Long tenantId);
}
