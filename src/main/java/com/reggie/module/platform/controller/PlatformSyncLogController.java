package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.platform.model.PlatformSyncLog;
import com.reggie.module.platform.service.PlatformSyncLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台同步日志控制器
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/sync-log")
@Tag(name = "平台同步日志")
@RequireEmployee
public class PlatformSyncLogController {

    @Autowired
    private PlatformSyncLogService syncLogService;

    /**
     * 分页查询同步日志
     *
     * @param page         页码
     * @param pageSize     每页条数
     * @param platformType 平台类型（可选）
     * @param action       动作类型（可选）
     * @param startTime    开始时间（可选）
     * @param endTime      结束时间（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "查询同步日志", description = "分页查询平台同步操作日志")
    public R<Page<PlatformSyncLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<PlatformSyncLog> result = syncLogService.page(page, pageSize, platformType, action, startTime, endTime);
        return R.success(result);
    }

    /**
     * 统计最近N小时的失败次数
     *
     * @param hours        小时数
     * @param platformType 平台类型（可选）
     * @return 失败次数
     */
    @GetMapping("/failure-count")
    @Operation(summary = "统计失败次数", description = "统计最近N小时的同步失败次数")
    public R<Map<String, Object>> countFailures(
            @RequestParam int hours,
            @RequestParam(required = false) String platformType) {
        long count = syncLogService.countFailuresInLastHours(hours, platformType);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("hours", hours);
        result.put("platformType", platformType);
        result.put("failureCount", count);
        return R.success(result);
    }

    /**
     * 查询异常订单
     *
     * @param platformType  平台类型
     * @param maxRetryCount 最大重试次数
     * @return 异常订单列表
     */
    @GetMapping("/abnormal")
    @Operation(summary = "查询异常订单", description = "查询失败且重试次数超过阈值的订单")
    public R<List<PlatformSyncLog>> getAbnormalOrders(
            @RequestParam String platformType,
            @RequestParam(defaultValue = "3") int maxRetryCount) {
        List<PlatformSyncLog> logs = syncLogService.getAbnormalOrders(platformType, maxRetryCount);
        return R.success(logs);
    }
}
