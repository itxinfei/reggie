package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.service.PrintTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 打印任务查询（后台）
 *
 * <p>门店 PC 打印代理执行的任务流水，支持按订单/类型/状态/时间筛选。
 * 总部超管可跨门店查看全部任务；门店员工仅查看本门店。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@RestController
@RequestMapping("/printer/task")
@RequireEmployee
@Tag(name = "打印任务查询（门店PC打印代理执行流水）")
public class PrintTaskController {

    @Autowired
    private PrintTaskService printTaskService;

    /**
     * 任务分页。
     *
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "打印任务分页")
    public R<IPage<PrintTask>> page(@RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
                                    @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int pageSize,
                                    @RequestParam(required = false) @Parameter(description = "订单ID") Long orderId,
                                    @RequestParam(required = false) @Parameter(description = "任务类型") String taskType,
                                    @RequestParam(required = false) @Parameter(description = "状态") String status,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                        @Parameter(description = "创建时间起") LocalDateTime beginTime,
                                    @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                        @Parameter(description = "创建时间止") LocalDateTime endTime,
                                    HttpServletRequest request) {
        return R.success(printTaskService.pageQuery(page, pageSize, resolveTenantId(request),
                orderId, taskType, status, beginTime, endTime));
    }

    /**
     * 任务统计：总数 / 今日 / 今日成功 / 今日失败 / 待处理。
     *
     * @return totalTasks / todayTotal / todaySuccess / todayFailed / pending
     */
    @GetMapping("/stats")
    @Operation(summary = "打印任务统计")
    public R<Map<String, Object>> stats(HttpServletRequest request) {
        return R.success(printTaskService.statTasks(resolveTenantId(request)));
    }

    /**
     * 租户视角：总部超管（roleKey=SUPER_ADMIN）看全部，门店员工看本门店租户。
     */
    private Long resolveTenantId(HttpServletRequest request) {
        String roleKey = (String) request.getAttribute("roleKey");
        return "SUPER_ADMIN".equals(roleKey) ? null : BaseContext.getCurrentTenantId();
    }
}
