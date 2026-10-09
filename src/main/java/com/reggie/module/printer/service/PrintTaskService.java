package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrintTask;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 打印任务服务（管理端任务查询 / 统计）
 *
 * @author AI
 * @since 2026-08-30
 */
public interface PrintTaskService extends IService<PrintTask> {

    /**
     * 打印任务统计（今日总数 / 成功 / 失败 / 待处理）。
     *
     * @param tenantId 租户ID（超管为 null 时统计全部）
     * @return totalTasks/todayTotal/todaySuccess/todayFailed/pending
     */
    Map<String, Object> statTasks(Long tenantId);

    /**
     * 任务分页（管理端）。
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @param tenantId  租户ID（超管为空=全部）
     * @param orderId   订单ID
     * @param taskType  任务类型
     * @param status    状态
     * @param beginTime 创建时间起
     * @param endTime   创建时间止
     * @return 分页结果
     */
    IPage<PrintTask> pageQuery(int page, int pageSize, Long tenantId, Long orderId, String taskType,
                               String status, LocalDateTime beginTime, LocalDateTime endTime);
}
