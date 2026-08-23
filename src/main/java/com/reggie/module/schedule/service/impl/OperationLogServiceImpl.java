package com.reggie.module.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.sys.model.OperationLog;
import com.reggie.module.sys.mapper.OperationLogMapper;
import com.reggie.module.schedule.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public void recordLog(OperationLog operationLog) {
        try {
            // 自动填充租户ID
            Long tenantId = BaseContext.getCurrentTenantId();
            if (tenantId != null) {
                operationLog.setTenantId(tenantId);
            }

            // 脱敏处理：如果描述中包含敏感信息
            if (operationLog.getDescription() != null) {
                operationLog.setDescription(LogMaskUtils.maskSensitiveInfo(operationLog.getDescription()));
            }
            if (operationLog.getRequestParams() != null) {
                operationLog.setRequestParams(LogMaskUtils.maskSensitiveInfo(operationLog.getRequestParams()));
            }

            save(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public Page<OperationLog> pageQuery(int page, int pageSize, String module,
                                         String operationType, String operatorName,
                                         LocalDateTime beginTime, LocalDateTime endTime) {
        Page<OperationLog> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.isEmpty()) {
            wrapper.like(OperationLog::getModule, module);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(OperationLog::getOperatorName, operatorName);
        }
        if (beginTime != null) {
            wrapper.ge(OperationLog::getCreateTime, beginTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(OperationLog::getCreateTime);
        Page<OperationLog> result = this.page(pageInfo, wrapper);

        // 脱敏处理
        result.getRecords().forEach(log -> {
            if (log.getRequestParams() != null) {
                log.setRequestParams(LogMaskUtils.maskSensitiveInfo(log.getRequestParams()));
            }
            if (log.getOldValue() != null) {
                log.setOldValue(LogMaskUtils.maskSensitiveInfo(log.getOldValue()));
            }
            if (log.getNewValue() != null) {
                log.setNewValue(LogMaskUtils.maskSensitiveInfo(log.getNewValue()));
            }
        });

        return result;
    }

    @Override
    public List<OperationLog> findByBizId(String tableName, Long bizId) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getTableName, tableName)
               .eq(OperationLog::getBizId, bizId)
               .orderByDesc(OperationLog::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public int cleanExpiredLogs(int retentionDays) {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(retentionDays);
        // 改为单条批量 UPDATE（跨租户系统维护），替代原先逐条更新，解决 N+1 性能问题与 fail-closed 空集问题
        int count = baseMapper.cleanExpiredLogsBatch(expireTime);

        log.info("[定时任务] 清理过期操作日志: 清理{}条，保留{}天", count, retentionDays);
        return count;
    }
}




