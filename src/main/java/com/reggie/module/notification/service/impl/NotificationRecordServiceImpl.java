package com.reggie.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.common.utils.PageUtils;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.notification.model.NotificationRecord;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.notification.service.NotificationRecordService;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(rollbackFor = Exception.class)
public class NotificationRecordServiceImpl
        extends ServiceImpl<NotificationRecordMapper, NotificationRecord>
        implements NotificationRecordService {

    /**
     * 分页查询 records。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param bizType 参数 bizType
     * @param status 参数 status
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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

    /**
     * 处理 stat between。
     * @param start 参数 start
     * @param end 参数 end
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> statBetween(LocalDateTime start, LocalDateTime end, Long tenantId) {
        return this.baseMapper.statBetween(start, end, tenantId);
    }

    /**
     * 获取 record with tenant check。
     * @param id 参数 id
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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