package com.reggie.module.schedule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.model.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 操作日志服务接口
 * </p>
 * <p>记录系统操作日志，支持分页查询和过期清理</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 记录操作日志
     */
    void recordLog(OperationLog log);

    /**
     * 分页查询操作日志
     */
    Page<OperationLog> pageQuery(int page, int pageSize, String module,
                                  String operationType, String operatorName,
                                  LocalDateTime beginTime, LocalDateTime endTime,
                                  Integer isSuccess);

    /**
     * 查询指定业务记录的操作日志
     */
    List<OperationLog> findByBizId(String tableName, Long bizId);

    /**
     * 清理过期日志（默认保留90天）
     */
    int cleanExpiredLogs(int retentionDays);
}

