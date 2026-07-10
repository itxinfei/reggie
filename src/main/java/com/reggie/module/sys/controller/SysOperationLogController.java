package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.OperationLog;
import com.reggie.module.schedule.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志查看Controller
 * 系统管理模块下的操作日志查看
 */
@Slf4j
@RestController
@RequestMapping("/sys/log")
@Tag(name = "系统管理-操作日志", description = "操作日志查询接口")
public class SysOperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 操作日志分页查询
     */
    @GetMapping("/page")
    @Operation(summary = "操作日志分页查询")
    public R<Page<OperationLog>> page(
            int page, int pageSize,
            @Parameter(description = "模块名称") String module,
            @Parameter(description = "操作类型：INSERT/UPDATE/DELETE/OTHER") String operationType,
            @Parameter(description = "操作人姓名") String operatorName,
            @Parameter(description = "开始时间") String beginTime,
            @Parameter(description = "结束时间") String endTime) {

        LocalDateTime beginDateTime = null;
        LocalDateTime endDateTime = null;

        if (beginTime != null && !beginTime.isEmpty()) {
            beginDateTime = LocalDateTime.parse(beginTime + "T00:00:00");
        }
        if (endTime != null && !endTime.isEmpty()) {
            endDateTime = LocalDateTime.parse(endTime + "T23:59:59");
        }

        Page<OperationLog> pageInfo = operationLogService.pageQuery(
                page, pageSize, module, operationType, operatorName, beginDateTime, endDateTime);
        return R.success(pageInfo);
    }

    /**
     * 查询指定业务记录的操作日志
     */
    @GetMapping("/biz")
    @Operation(summary = "查询业务操作日志")
    public R<List<OperationLog>> getByBizId(
            @Parameter(description = "表名") String tableName,
            @Parameter(description = "业务记录ID") Long bizId) {
        List<OperationLog> logs = operationLogService.findByBizId(tableName, bizId);
        return R.success(logs);
    }

    /**
     * 操作日志统计
     */
    @GetMapping("/stats")
    @Operation(summary = "操作日志统计")
    public R<Map<String, Object>> stats() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        // 今日操作数
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog> todayWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        todayWrapper.ge(OperationLog::getCreateTime, todayStart);
        stats.put("todayCount", operationLogService.count(todayWrapper));

        // 总操作数
        stats.put("totalCount", operationLogService.count());

        return R.success(stats);
    }
}
