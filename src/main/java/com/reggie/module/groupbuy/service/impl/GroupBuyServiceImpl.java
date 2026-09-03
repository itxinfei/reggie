package com.reggie.module.groupbuy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.groupbuy.mapper.GroupBuyCampaignMapper;
import com.reggie.module.groupbuy.mapper.GroupBuyParticipationMapper;
import com.reggie.module.groupbuy.model.GroupBuyCampaign;
import com.reggie.module.groupbuy.model.GroupBuyParticipation;
import com.reggie.module.groupbuy.service.GroupBuyService;
import com.reggie.module.payment.service.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 拼团活动服务实现
 *
 * @author reggie
 * @since 2026-09-01
 */
@Service
@Slf4j
public class GroupBuyServiceImpl extends ServiceImpl<GroupBuyCampaignMapper, GroupBuyCampaign> implements GroupBuyService {

    @Autowired
    private GroupBuyParticipationMapper participationMapper;

    @Autowired
    private RefundService refundService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuyCampaign createCampaign(GroupBuyCampaign campaign) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        campaign.setTenantId(tenantId);
        campaign.setStatus("OPEN");
        campaign.setIsDeleted(0);
        campaign.setCreateTime(LocalDateTime.now());
        campaign.setUpdateTime(LocalDateTime.now());
        save(campaign);
        return campaign;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuyCampaign updateCampaign(GroupBuyCampaign campaign) {
        if (campaign.getId() == null) {
            throw new CustomException("拼团活动ID不能为空");
        }
        GroupBuyCampaign exist = getById(campaign.getId());
        if (exist == null) {
            throw new CustomException("拼团活动不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的拼团活动");
        }
        exist.setName(campaign.getName());
        exist.setDescription(campaign.getDescription());
        exist.setGroupId(campaign.getGroupId());
        exist.setStartTime(campaign.getStartTime());
        exist.setEndTime(campaign.getEndTime());
        exist.setMinMembers(campaign.getMinMembers());
        exist.setMaxMembers(campaign.getMaxMembers());
        exist.setOriginalPrice(campaign.getOriginalPrice());
        exist.setGroupPrice(campaign.getGroupPrice());
        exist.setDishId(campaign.getDishId());
        exist.setDishName(campaign.getDishName());
        exist.setImage(campaign.getImage());
        exist.setUpdateTime(LocalDateTime.now());
        updateById(exist);
        return exist;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCampaign(Long id) {
        GroupBuyCampaign exist = getById(id);
        if (exist == null) {
            throw new CustomException("拼团活动不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null && !tenantId.equals(exist.getTenantId())) {
            throw new CustomException("无权操作其他租户的拼团活动");
        }
        removeById(id);
    }

    @Override
    public Page<GroupBuyCampaign> listCampaigns(int page, int pageSize, String name) {
        Page<GroupBuyCampaign> pageRequest = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<GroupBuyCampaign> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            qw.like(GroupBuyCampaign::getName, name);
        }
        qw.orderByDesc(GroupBuyCampaign::getCreateTime);
        Page<GroupBuyCampaign> result = page(pageRequest, qw);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupBuyParticipation joinGroupBuy(Long campaignId, Long orderId, Long userId) {
        GroupBuyCampaign campaign = getById(campaignId);
        if (campaign == null) {
            throw new CustomException("拼团活动不存在");
        }
        if (!"OPEN".equals(campaign.getStatus())) {
            throw new CustomException("拼团活动未开放");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartTime()) || now.isAfter(campaign.getEndTime())) {
            throw new CustomException("拼团活动不在有效期内");
        }

        GroupBuyParticipation participation = new GroupBuyParticipation();
        participation.setTenantId(BaseContext.getCurrentTenantId());
        participation.setGroupBuyId(campaignId);
        participation.setOrderId(orderId);
        participation.setUserId(userId);
        participation.setStatus("JOINED");
        participation.setJoinTime(now);
        participation.setCreateTime(now);
        participationMapper.insert(participation);
        return participation;
    }

    @Override
    public boolean checkGroupBuyStatus(Long campaignId) {
        GroupBuyCampaign campaign = getById(campaignId);
        if (campaign == null) {
            return false;
        }
        int count = participationMapper.countParticipants(campaignId);
        return count >= campaign.getMinMembers();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markParticipationPaid(Long orderId) {
        LambdaQueryWrapper<GroupBuyParticipation> qw = new LambdaQueryWrapper<>();
        qw.eq(GroupBuyParticipation::getOrderId, orderId);
        qw.eq(GroupBuyParticipation::getStatus, "JOINED");
        GroupBuyParticipation participation = participationMapper.selectOne(qw);
        // 幂等：非拼团单或已支付的订单无 JOINED 记录，直接跳过，供支付回调安全统一调用
        if (participation == null) {
            return;
        }
        participation.setStatus("PAID");
        participation.setPayTime(LocalDateTime.now());
        participationMapper.updateById(participation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCloseExpiredCampaigns() {
        // 委托统一的成团/未成团判定，避免与 scanGroupFormedAndNotFormed 语义分裂：
        // 旧实现把过期 OPEN 全标 ENDED，会误伤已达标的成团活动（应 CLOSED）且漏退款。
        // this 调用共享当前事务，无需走代理；scan 内部退款用独立事务，互不影响。
        return scanGroupFormedAndNotFormed();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanGroupFormedAndNotFormed() {
        // 拉取所有已结束且处于 OPEN 的 campaign（endTime 到，但未做成团/未成团判定）
        LambdaQueryWrapper<GroupBuyCampaign> qw = new LambdaQueryWrapper<>();
        qw.eq(GroupBuyCampaign::getStatus, "OPEN");
        qw.le(GroupBuyCampaign::getEndTime, LocalDateTime.now());
        List<GroupBuyCampaign> campaigns = list(qw);
        if (campaigns.isEmpty()) {
            return 0;
        }
        int handled = 0;
        for (GroupBuyCampaign campaign : campaigns) {
            int paidCount = participationMapper.countPaidParticipants(campaign.getId());
            if (paidCount >= campaign.getMinMembers()) {
                // 成团：标记 CLOSED，下游可据此发券/打标签/履约
                campaign.setStatus("CLOSED");
            } else {
                // 未成团：标记 ENDED，触发参与订单退款
                campaign.setStatus("ENDED");
                refundNotFormedParticipants(campaign);
            }
            campaign.setUpdateTime(LocalDateTime.now());
            updateById(campaign);
            handled++;
        }
        return handled;
    }

    /**
     * 未成团场景：对已支付参与者的订单发起全额退款，幂等重试安全。
     * <p>
     * refundByOrder 内部已判断订单是否可退（STATUS=6 REFUND 等直接返回 false）；
     * 此处 try-catch 包裹，单条失败不影响其它 campaign/订单。
     * </p>
     */
    private void refundNotFormedParticipants(GroupBuyCampaign campaign) {
        List<GroupBuyParticipation> participants = participationMapper.selectList(
                new LambdaQueryWrapper<GroupBuyParticipation>()
                        .eq(GroupBuyParticipation::getGroupBuyId, campaign.getId())
                        .eq(GroupBuyParticipation::getStatus, "PAID"));
        if (participants == null || participants.isEmpty()) {
            return;
        }
        for (GroupBuyParticipation p : participants) {
            try {
                refundService.refundByOrder(p.getOrderId(), "拼团未成团自动退款");
            } catch (Exception e) {
                log.error("[拼团] 未成团退款失败: campaignId={}, orderId={}, error={}",
                        campaign.getId(), p.getOrderId(), e.getMessage());
            }
        }
    }
}
