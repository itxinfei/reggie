package com.reggie.module.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.platform.model.PlatformSyncLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台同步日志服务
 *
 * @author reggie
 * @since 2026-08-24
 */
public interface PlatformSyncLogService extends IService<PlatformSyncLog> {

    /**
     * 分页查询同步日志
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param platformType 平台类型（可选）
     * @param action     动作类型（可选）
     * @param startTime  开始时间（可选）
     * @param endTime    结束时间（可选）
     * @return 分页结果
     */
    Page<PlatformSyncLog> page(int page, int pageSize, String platformType, String action,
                               LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计最近N小时的失败次数
     *
     * @param hours  小时数
     * @param platformType  平台类型（可选）
     * @return 失败次数
     */
    long countFailuresInLastHours(int hours, String platformType);

    /**
     * 查询指定平台的异常订单（失败且未重试超过阈值）
     *
     * @param platformType 平台类型
     * @param maxRetryCount 最大重试次数
     * @return 异常订单列表
     */
    List<PlatformSyncLog> getAbnormalOrders(String platformType, int maxRetryCount);
}
