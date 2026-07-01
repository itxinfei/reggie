package com.reggie.module.report.service;

import java.util.List;
import java.util.Map;

public interface ReportService {
    Map<String, Object> getDailyReport(String date);
    List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit);
    List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate);
    Map<String, Object> getPaymentAnalysis(String startDate, String endDate);
    byte[] exportDailyReport(String startDate, String endDate);
}
