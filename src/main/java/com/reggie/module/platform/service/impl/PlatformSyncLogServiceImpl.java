package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.common.BaseContext;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.common.utils.PageUtils;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.platform.mapper.PlatformSyncLogMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.platform.model.PlatformSyncLog;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.platform.service.PlatformSyncLogService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/**
 * 平台同步日志服务实现
 *
 * @author reggie
 * @since 2026-08-24
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PlatformSyncLogServiceImpl extends ServiceImpl<PlatformSyncLogMapper, PlatformSyncLog> implements
        PlatformSyncLogService {

    /**
     * 分页查询。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param platformType 参数 platformType
     * @param action 参数 action
     * @param startTime 参数 startTime
     * @param endTime 参数 endTime
     * @return 返回结果
     */
    @Override
    public Page<PlatformSyncLog> page(int page, int pageSize, String platformType, String action,
                                      LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformSyncLog::getTenantId, BaseContext.getCurrentTenantId());
        qw.eq(platformType != null && !platformType.isEmpty(), PlatformSyncLog::getPlatformType, platformType);
        qw.eq(action != null && !action.isEmpty(), PlatformSyncLog::getAction, action);
        qw.ge(startTime != null, PlatformSyncLog::getCreateTime, startTime);
        qw.le(endTime != null, PlatformSyncLog::getCreateTime, endTime);
        qw.orderByDesc(PlatformSyncLog::getCreateTime);
        return page(PageUtils.of(page, pageSize), qw);
    }

    /**
     * 统计 failures in last hours。
     * @param hours 参数 hours
     * @param platformType 参数 platformType
     * @return 返回结果
     */
    @Override
    public long countFailuresInLastHours(int hours, String platformType) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformSyncLog::getTenantId, BaseContext.getCurrentTenantId());
        qw.eq(PlatformSyncLog::getStatus, 1); // 失败
        qw.ge(PlatformSyncLog::getCreateTime, startTime);
        if (platformType != null && !platformType.isEmpty()) {
            qw.eq(PlatformSyncLog::getPlatformType, platformType);
        }
        return count(qw);
    }

    /**
     * 获取 abnormal orders。
     * @param platformType 参数 platformType
     * @param maxRetryCount 参数 maxRetryCount
     * @return 返回结果
     */
    @Override
    public List<PlatformSyncLog> getAbnormalOrders(String platformType, int maxRetryCount) {
        LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformSyncLog::getTenantId, BaseContext.getCurrentTenantId());
        qw.eq(PlatformSyncLog::getStatus, 1); // 失败
        qw.ge(PlatformSyncLog::getRetryCount, maxRetryCount);
        if (platformType != null && !platformType.isEmpty()) {
            qw.eq(PlatformSyncLog::getPlatformType, platformType);
        }
        qw.orderByDesc(PlatformSyncLog::getCreateTime);
        return list(qw);
    }
}
