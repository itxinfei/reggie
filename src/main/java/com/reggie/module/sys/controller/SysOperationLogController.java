package com.reggie.module.sys.controller;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.module.sys.model.OperationLog;
import com.reggie.module.schedule.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 操作日志查看Controller
 * 系统管理模块下的操作日志查看
 * </p>
 *
 * 安全加固（2026-08-23）：
 * getByBizId() 原直接接受 String tableName 作为 @RequestParam，虽 Service 层使用 LambdaQueryWrapper
 * （不会导致 SQL 注入），但攻击者可构造任意表名（如 information_schema.tables）进行数据探测。
 * 现添加表名白名单校验，仅允许已知的业务表名。
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

    /**
     * 业务表名白名单：仅允许查询这些表的操作日志
     * 白名单内均为本系统已知的业务表，禁止任意表名传入
     */
    private static final Set<String> ALLOWED_TABLE_NAMES = new HashSet<>(Arrays.asList(
            "employee",
            "user",
            "dish",
            "setmeal",
            "category",
            "dish_flavor",
            "setmeal_dish",
            "orders",
            "order_detail",
            "address_book",
            "coupon_template",
            "coupon_user",
            "points_record",
            "member",
            "member_level",
            "payment_order",
            "refund_record",
            "stock_check",
            "stock_check_item",
            "purchase_order",
            "material",
            "supplier",
            "dining_table",
            "table_area",
            "reservation",
            "queue_record",
            "ai_conversation",
            "ai_message",
            "operation_log",
            "system_config",
            "role",
            "permission",
            "tenant",
            "printer_config",
            "notification_template",
            "delivery_rider",
            "delivery_order",
            "recommendation_cache",
            "marketing_campaign",
            "franchise",
            "franchise_settlement",
            "ai_user_profile"
    ));

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
                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "模块名称") String module,
            @Parameter(description = "操作类型：INSERT/UPDATE/DELETE/OTHER") String operationType,
            @Parameter(description = "操作人姓名") String operatorName,
            @Parameter(description = "开始时间(yyyy-MM-dd)") String beginTime,
            @Parameter(description = "结束时间(yyyy-MM-dd)") String endTime,
            @Parameter(description = "是否成功：1=成功 0=失败") Integer isSuccess) {

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
                page, pageSize, module, operationType, operatorName, beginDateTime, endDateTime, isSuccess);
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
            @Parameter(description = "表名") @RequestParam String tableName,
            @Parameter(description = "业务记录ID") @RequestParam Long bizId) {
        // 安全校验：表名必须来自白名单，防止攻击者探测任意表
        if (tableName == null || tableName.trim().isEmpty()) {
            return R.error("表名不能为空");
        }
        if (bizId == null || bizId <= 0) {
            return R.error("业务记录ID无效");
        }
        if (!ALLOWED_TABLE_NAMES.contains(tableName)) {
            log.warn("[操作日志] 拒绝非法表名查询: tableName={}, bizId={}", tableName, bizId);
            return R.error("不允许查询该业务表的操作日志");
        }
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



