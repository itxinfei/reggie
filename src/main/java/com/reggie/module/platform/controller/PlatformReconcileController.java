package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.platform.model.PlatformReconcileTask;
import com.reggie.module.platform.service.PlatformReconcileTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 平台对账管理控制器
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@RestController
@RequestMapping("/api/platform/reconcile")
@Tag(name = "平台对账管理")
@RequireEmployee
public class PlatformReconcileController {

    @Autowired
    private PlatformReconcileTaskService reconcileTaskService;

    /**
     * 执行对账任务
     *
     * @param platformType 平台类型（MEITUAN/ELEME/DOUYIN）
     * @param date         对账日期
     * @return 对账结果
     */
    @PostMapping("/execute")
    @Operation(summary = "执行对账", description = "对指定平台和日期执行订单对账")
    public R<PlatformReconcileTask> executeReconcile(
            @RequestParam String platformType,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        log.info("执行对账: platformType={}, date={}", platformType, date);
        PlatformReconcileTask task = reconcileTaskService.reconcile(platformType, date);
        return R.success(task);
    }

    /**
     * 查询对账任务
     *
     * @param platformType 平台类型
     * @param date         对账日期
     * @return 对账任务
     */
    @GetMapping("/query")
    @Operation(summary = "查询对账", description = "查询指定平台和日期的对账任务结果")
    public R<PlatformReconcileTask> queryReconcile(
            @RequestParam String platformType,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        PlatformReconcileTask task = reconcileTaskService.getByDate(platformType, date);
        if (task == null) {
            return R.error("未找到对账记录");
        }
        return R.success(task);
    }
}
