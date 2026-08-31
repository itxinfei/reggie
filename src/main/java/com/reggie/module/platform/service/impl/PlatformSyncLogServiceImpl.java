package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.platform.mapper.PlatformSyncLogMapper;
import com.reggie.module.platform.model.PlatformSyncLog;
import com.reggie.module.platform.service.PlatformSyncLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台同步日志服务实现
 *
 * @author reggie
 * @since 2026-08-24
 */
@Service
public class PlatformSyncLogServiceImpl extends ServiceImpl<PlatformSyncLogMapper, PlatformSyncLog> implements PlatformSyncLogService {

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
