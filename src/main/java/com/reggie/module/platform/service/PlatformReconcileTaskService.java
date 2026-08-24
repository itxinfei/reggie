package com.reggie.module.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.platform.model.PlatformReconcileTask;

import java.time.LocalDate;

/**
 * 平台对账任务服务
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformReconcileTaskService extends IService<PlatformReconcileTask> {

    /**
     * 执行对账任务（比对平台订单与本地订单）
     *
     * @param platformType 平台类型
     * @param date         对账日期
     * @return 对账结果
     */
    PlatformReconcileTask reconcile(String platformType, LocalDate date);

    /**
     * 查询指定日期的对账任务
     *
     * @param platformType 平台类型
     * @param date         对账日期
     * @return 对账任务
     */
    PlatformReconcileTask getByDate(String platformType, LocalDate date);
}
