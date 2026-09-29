package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.CustomException;
import com.reggie.module.delivery.mapper.RiderMessageMapper;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderMessage;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.delivery.service.RiderMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 骑手消息服务实现。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@Service
public class RiderMessageServiceImpl implements RiderMessageService {

    @Autowired
    private RiderMessageMapper messageMapper;

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderMessage send(Long riderId, Long tenantId, Integer type, String title,
                            String content, Long bizId) {
        if (riderId == null) {
            throw new CustomException("接收骑手不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new CustomException("消息标题不能为空");
        }
        RiderMessage m = new RiderMessage();
        m.setTenantId(tenantId);
        m.setRiderId(riderId);
        m.setType(type == null ? RiderMessage.TYPE_OTHER : type);
        m.setTitle(title);
        m.setContent(content);
        m.setBizId(bizId);
        m.setIsRead(RiderMessage.UNREAD);
        messageMapper.insert(m);
        log.info("骑手消息已写入：riderId={}, type={}, bizId={}", riderId, m.getType(), bizId);
        return m;
    }

    @Override
    public Page<RiderMessage> pageMine(Long riderId, Long tenantId, int page, int size) {
        Page<RiderMessage> p = new Page<>(page, size);
        LambdaQueryWrapper<RiderMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderMessage::getRiderId, riderId)
                .eq(RiderMessage::getTenantId, tenantId)
                .eq(RiderMessage::getIsDeleted, 0)
                .orderByDesc(RiderMessage::getCreateTime);
        return messageMapper.selectPage(p, qw);
    }

    @Override
    public long unreadCount(Long riderId, Long tenantId) {
        LambdaQueryWrapper<RiderMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderMessage::getRiderId, riderId)
                .eq(RiderMessage::getTenantId, tenantId)
                .eq(RiderMessage::getIsRead, RiderMessage.UNREAD)
                .eq(RiderMessage::getIsDeleted, 0);
        return messageMapper.selectCount(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderMessage markRead(Long id, Long riderId, Long tenantId) {
        RiderMessage m = messageMapper.selectById(id);
        if (m == null || Objects.equals(m.getIsDeleted(), 1)) {
            throw new CustomException("消息不存在");
        }
        if (!Objects.equals(m.getRiderId(), riderId)) {
            throw new CustomException("无权操作他人的消息");
        }
        if (tenantId != null && !Objects.equals(tenantId, m.getTenantId())) {
            throw new CustomException("无权操作其他租户的消息");
        }
        if (Objects.equals(m.getIsRead(), RiderMessage.READ)) {
            return m;  // 幂等：重复标记不报错
        }
        m.setIsRead(RiderMessage.READ);
        m.setReadTime(LocalDateTime.now());
        messageMapper.updateById(m);
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead(Long riderId, Long tenantId) {
        LambdaUpdateWrapper<RiderMessage> uw = new LambdaUpdateWrapper<>();
        uw.eq(RiderMessage::getRiderId, riderId)
                .eq(RiderMessage::getTenantId, tenantId)
                .eq(RiderMessage::getIsRead, RiderMessage.UNREAD)
                .eq(RiderMessage::getIsDeleted, 0)
                .set(RiderMessage::getIsRead, RiderMessage.READ)
                .set(RiderMessage::getReadTime, LocalDateTime.now());
        return messageMapper.update(null, uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int broadcast(Long tenantId, String title, String content) {
        if (!StringUtils.hasText(title)) {
            throw new CustomException("公告标题不能为空");
        }
        List<Rider> riders = deliveryTrackingService.getRiderList(null, tenantId);
        if (riders == null || riders.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (Rider r : riders) {
            if (r == null || r.getId() == null) {
                continue;
            }
            send(r.getId(), tenantId, RiderMessage.TYPE_NOTICE, title, content, null);
            n++;
        }
        log.info("骑手公告已广播：tenantId={}, count={}", tenantId, n);
        return n;
    }
}
