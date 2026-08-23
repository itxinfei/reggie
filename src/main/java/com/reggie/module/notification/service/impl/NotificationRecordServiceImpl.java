package com.reggie.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.service.NotificationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 通知发送记录服务实现
 * </p>
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@Service
public class NotificationRecordServiceImpl
        extends ServiceImpl<NotificationRecordMapper, NotificationRecord>
        implements NotificationRecordService {

    @Override
    public Page<NotificationRecord> pageRecords(int page, int pageSize, String bizType,
                                                 Integer status, Long tenantId) {
        Page<NotificationRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<NotificationRecord> wrapper = new LambdaQueryWrapper<>();
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(NotificationRecord::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(NotificationRecord::getStatus, status);
        }
        if (tenantId != null) {
            wrapper.eq(NotificationRecord::getTenantId, tenantId);
        }
        wrapper.orderByDesc(NotificationRecord::getCreateTime);
        this.page(pageInfo, wrapper);
        return pageInfo;
    }

    @Override
    public Map<String, Object> statBetween(LocalDateTime start, LocalDateTime end, Long tenantId) {
        return this.baseMapper.statBetween(start, end, tenantId);
    }

    @Override
    public Map<String, Object> getRecordWithTenantCheck(Long id, Long tenantId) {
        NotificationRecord record = this.getById(id);
        if (record == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "记录不存在");
            return result;
        }
        if (tenantId != null && !tenantId.equals(record.getTenantId())) {
            Map<String, Object> result = new HashMap<>();
            result.put("ok", false);
            result.put("message", "无权查看其他租户的通知记录");
            return result;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("record", record);
        return result;
    }
}