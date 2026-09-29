package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.mapper.RiderEvaluationMapper;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderEvaluation;
import com.reggie.module.delivery.service.RiderEvaluationService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * 骑手评价服务实现。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@Service
public class RiderEvaluationServiceImpl extends ServiceImpl<RiderEvaluationMapper, RiderEvaluation>
        implements RiderEvaluationService {

    @Autowired
    private OrderMapper ordersMapper;

    @Autowired
    private com.reggie.module.delivery.mapper.RiderMapper riderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiderEvaluation submit(RiderEvaluation evaluation) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        if (tenantId == null || userId == null) {
            throw new CustomException("请先登录");
        }
        if (evaluation.getOrderId() == null) {
            throw new CustomException("订单ID不能为空");
        }
        if (evaluation.getRiderId() == null) {
            throw new CustomException("骑手ID不能为空");
        }
        Integer star = evaluation.getStarRating();
        if (star == null || star < 1 || star > 5) {
            throw new CustomException("评分需在 1-5 分之间");
        }

        Orders order = ordersMapper.selectById(evaluation.getOrderId());
        if (order == null || !tenantId.equals(order.getTenantId())) {
            throw new CustomException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new CustomException("只能评价自己的订单");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_COMPLETED)) {
            throw new CustomException("订单未完成，无法评价骑手");
        }
        if (!ObjectUtils.nullSafeEquals(order.getRiderId(), evaluation.getRiderId())) {
            throw new CustomException("该订单不是由该骑手配送");
        }

        // 幂等：同一订单对同一骑手仅一次有效评价
        if (getByOrderAndRider(tenantId, evaluation.getOrderId(), evaluation.getRiderId()) != null) {
            throw new CustomException("该订单已评价过骑手");
        }

        Rider rider = riderMapper.selectById(evaluation.getRiderId());
        evaluation.setUserId(userId);
        evaluation.setUserName(order.getUserName());
        evaluation.setRiderName(rider != null ? rider.getName() : null);
        evaluation.setAnonymous(evaluation.getAnonymous() == null ? 0 : evaluation.getAnonymous());
        evaluation.setStatus(1);
        this.save(evaluation);

        refreshRiderRating(evaluation.getRiderId(), tenantId);
        log.info("顾客提交骑手评价：orderId={}, riderId={}, star={}", evaluation.getOrderId(),
                evaluation.getRiderId(), star);
        return evaluation;
    }

    @Override
    public RiderEvaluation getByOrderAndRider(Long tenantId, Long orderId, Long riderId) {
        LambdaQueryWrapper<RiderEvaluation> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderEvaluation::getOrderId, orderId)
                .eq(RiderEvaluation::getRiderId, riderId)
                .eq(RiderEvaluation::getIsDeleted, 0)
                .orderByDesc(RiderEvaluation::getCreateTime)
                .last("LIMIT 1");
        return this.getOne(qw);
    }

    @Override
    public Page<RiderEvaluation> pageByUserId(Long tenantId, Long userId, int page, int size) {
        LambdaQueryWrapper<RiderEvaluation> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderEvaluation::getTenantId, tenantId)
                .eq(RiderEvaluation::getUserId, userId)
                .eq(RiderEvaluation::getIsDeleted, 0)
                .orderByDesc(RiderEvaluation::getCreateTime);
        return this.page(PageUtils.of(page, PageUtils.cap(size)), qw);
    }

    @Override
    public Page<RiderEvaluation> pageByRiderId(Long tenantId, Long riderId, int page, int size) {
        LambdaQueryWrapper<RiderEvaluation> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderEvaluation::getTenantId, tenantId)
                .eq(RiderEvaluation::getRiderId, riderId)
                .eq(RiderEvaluation::getStatus, 1)
                .eq(RiderEvaluation::getIsDeleted, 0)
                .orderByDesc(RiderEvaluation::getCreateTime);
        return this.page(PageUtils.of(page, PageUtils.cap(size)), qw);
    }

    @Override
    public Page<RiderEvaluation> pageReceived(Long tenantId, Long riderId, int page, int size) {
        LambdaQueryWrapper<RiderEvaluation> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderEvaluation::getTenantId, tenantId)
                .eq(RiderEvaluation::getRiderId, riderId)
                .eq(RiderEvaluation::getIsDeleted, 0)
                .orderByDesc(RiderEvaluation::getCreateTime);
        return this.page(PageUtils.of(page, PageUtils.cap(size)), qw);
    }

    @Override
    public Map<String, Object> getRiderStats(Long tenantId, Long riderId) {
        return baseMapper.selectRiderStats(riderId, tenantId);
    }

    @Override
    public Page<RiderEvaluation> adminPage(Long tenantId, String riderName, Integer starRating,
                                          Integer status, int page, int size) {
        LambdaQueryWrapper<RiderEvaluation> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderEvaluation::getTenantId, tenantId)
                .eq(RiderEvaluation::getIsDeleted, 0);
        if (!ObjectUtils.isEmpty(riderName)) {
            qw.like(RiderEvaluation::getRiderName, riderName);
        }
        if (starRating != null) {
            qw.eq(RiderEvaluation::getStarRating, starRating);
        }
        if (status != null) {
            qw.eq(RiderEvaluation::getStatus, status);
        }
        qw.orderByDesc(RiderEvaluation::getCreateTime);
        return this.page(PageUtils.of(page, PageUtils.cap(size)), qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reply(Long id, String replyContent, Long operatorId, Long tenantId) {
        if (replyContent == null || replyContent.trim().isEmpty()) {
            throw new CustomException("回复内容不能为空");
        }
        RiderEvaluation ev = this.getById(id);
        if (ev == null || !tenantId.equals(ev.getTenantId())) {
            throw new CustomException("评价不存在");
        }
        ev.setReplyContent(replyContent);
        ev.setReplyTime(java.time.LocalDateTime.now());
        return this.updateById(ev);
    }

    /**
     * 重算并更新骑手平均分与评分数（仅统计已通过评价）。
     */
    private void refreshRiderRating(Long riderId, Long tenantId) {
        Map<String, Object> stats = baseMapper.selectRiderStats(riderId, tenantId);
        BigDecimal avg = BigDecimal.ZERO;
        if (stats.get("avg") != null) {
            avg = new BigDecimal(stats.get("avg").toString()).setScale(1, RoundingMode.HALF_UP);
        }
        int cnt = 0;
        if (stats.get("cnt") != null) {
            cnt = ((Number) stats.get("cnt")).intValue();
        }
        Rider rider = new Rider();
        rider.setRating(avg);
        LambdaQueryWrapper<Rider> uw = new LambdaQueryWrapper<>();
        uw.eq(Rider::getId, riderId).eq(Rider::getTenantId, tenantId);
        riderMapper.update(rider, uw);
    }
}
