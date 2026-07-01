package com.reggie.module.report.controller;

import com.reggie.common.R;
import com.reggie.module.report.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@Slf4j
@Tag(name = "经营报表")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/daily")
    public R<Map<String, Object>> dailyReport(@RequestParam String date) {
        Map<String, Object> data = reportService.getDailyReport(date);
        return R.success(data);
    }

    @GetMapping("/dish-ranking")
    public R<List<Map<String, Object>>> dishRanking(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = reportService.getDishRanking(startDate, endDate, limit);
        return R.success(data);
    }

    @GetMapping("/time-slot")
    public R<List<Map<String, Object>>> timeSlotAnalysis(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<Map<String, Object>> data = reportService.getTimeSlotAnalysis(startDate, endDate);
        return R.success(data);
    }

    @GetMapping("/payment")
    public R<Map<String, Object>> paymentAnalysis(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Map<String, Object> data = reportService.getPaymentAnalysis(startDate, endDate);
        return R.success(data);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        byte[] data = reportService.exportDailyReport(startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("report.csv").build());
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
