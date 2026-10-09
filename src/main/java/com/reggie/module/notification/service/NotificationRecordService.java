package com.reggie.module.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.notification.model.NotificationRecord;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 通知发送记录服务接口
 * <p>域4 改造：从 NotificationController 下沉，Controller 不再直接操作 Mapper</p>
 *
 * @author reggie
 * @since 2026-08-22
 */
public interface NotificationRecordService extends IService<NotificationRecord> {

    /**
     * 发送记录分页查询
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param bizType  业务类型（可空）
     * @param status   状态（可空）
     * @param tenantId 当前租户ID
     * @return 分页结果
     */
    Page<NotificationRecord> pageRecords(int page, int pageSize, String bizType, Integer status, Long tenantId);

    /**
     * 指定时间范围内的发送记录聚合统计
     *
     * @param start    起始时间
     * @param end      结束时间
     * @param tenantId 当前租户ID
     * @return 聚合结果：todaySms/todayPush/todaySuccess/todayFail
     */
    Map<String, Object> statBetween(LocalDateTime start, LocalDateTime end, Long tenantId);

    /**
     * 查询发送记录并校验租户归属
     *
     * @param id       记录ID
     * @param tenantId 当前租户ID
     * @return 校验结果：key="ok"/"error"，ok 时附 record，error 时附 message
     */
    Map<String, Object> getRecordWithTenantCheck(Long id, Long tenantId);
}