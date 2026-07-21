package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.entity.OperationLog;
import com.reggie.module.schedule.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 操作日志查看Controller
 * 系统管理模块下的操作日志查看
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RequiresAdmin
@RestController
@RequestMapping("/sys/log")
@Tag(name = "系统管理-操作日志", description = "操作日志查询接口")
public class SysOperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 操作日志分页查询
     * @param page 页码
     * @param pageSize 每页条数
     * @param module 模块名称
     * @param operationType 操作类型
     * @param operatorName 操作人姓名
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "操作日志分页查询")
    public R<Page<OperationLog>> page(
            // 修改点：补充分页默认值，避免未传参时 page/pageSize 为 0 导致分页异常
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "模块名称") String module,
            @Parameter(description = "操作类型：INSERT/UPDATE/DELETE/OTHER") String operationType,
            @Parameter(description = "操作人姓名") String operatorName,
            @Parameter(description = "开始时间(yyyy-MM-dd)") String beginTime,
            @Parameter(description = "结束时间(yyyy-MM-dd)") String endTime) {

        // 修改点：日期格式与区间校验，避免 LocalDateTime.parse 抛异常导致 500
        LocalDateTime beginDateTime = null;
        LocalDateTime endDateTime = null;
        try {
            if (beginTime != null && !beginTime.isEmpty()) {
                beginDateTime = LocalDate.parse(beginTime, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
            }
            if (endTime != null && !endTime.isEmpty()) {
                endDateTime = LocalDate.parse(endTime, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59, 59);
            }
        } catch (DateTimeParseException e) {
            log.warn("[操作日志] 日期格式错误：beginTime={}, endTime={}", beginTime, endTime, e);
            return R.error("日期格式错误，请使用 yyyy-MM-dd");
        }
        if (beginDateTime != null && endDateTime != null && beginDateTime.isAfter(endDateTime)) {
            return R.error("开始时间不能晚于结束时间");
        }

        Page<OperationLog> pageInfo = operationLogService.pageQuery(
                page, pageSize, module, operationType, operatorName, beginDateTime, endDateTime);
        return R.success(pageInfo);
    }

    /**
     * 查询指定业务记录的操作日志
     * @param tableName 表名
     * @param bizId 业务记录ID
     * @return 操作日志列表
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
     * @return 统计信息（今日操作数、总操作数）
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
