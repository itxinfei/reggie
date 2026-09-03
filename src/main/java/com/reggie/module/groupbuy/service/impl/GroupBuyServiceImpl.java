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
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

    /**
     * 支付单服务（@Lazy 避免与 PaymentOrderServiceImpl 注入 GroupBuyService 形成循环依赖）。
     * 用于 scan 自愈：查 JOINED 参与的订单是否已成功支付，补偿标记 PAID（F1）。
     */
    @Autowired
    @Lazy
    private PaymentOrderService paymentOrderService;

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
            // F1 自愈：markParticipationPaid 首次失败致 participation 卡 JOINED 时，
            // scan 过期判定前先补偿——查其订单支付单 SUCCESS 则 JOINED→PAID，避免误判未成团漏退
            healJoinedParticipations(campaign);
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

    /**
     * 自愈卡在 JOINED 的参与：若其订单已有 SUCCESS 支付单，补偿标记 PAID（F1）。
     * <p>
     * 场景：支付回调时 markParticipationPaid（REQUIRES_NEW）因 DB 抖动失败被
     * PaymentOrderServiceImpl 静默 catch，participation 留 JOINED。若不补偿，
     * countPaidParticipants（仅计 PAID）会误判未成团，refundNotFormedParticipants
     * 又只退 PAID，导致已付款订单既未成团也未退款，资金卡死。
     * 此处在 scan 过期判定前补偿，markParticipationPaid 幂等，重复执行安全。
     * </p>
     */
    private void healJoinedParticipations(GroupBuyCampaign campaign) {
        List<GroupBuyParticipation> joinedList = participationMapper.selectList(
                new LambdaQueryWrapper<GroupBuyParticipation>()
                        .eq(GroupBuyParticipation::getGroupBuyId, campaign.getId())
                        .eq(GroupBuyParticipation::getStatus, "JOINED"));
        if (joinedList == null || joinedList.isEmpty()) {
            return;
        }
        for (GroupBuyParticipation p : joinedList) {
            try {
                PaymentOrder successPo = paymentOrderService.lambdaQuery()
                        .eq(PaymentOrder::getOrderId, p.getOrderId())
                        .eq(PaymentOrder::getStatus, PaymentOrder.STATUS_SUCCESS)
                        .orderByDesc(PaymentOrder::getId)
                        .last("limit 1")
                        .one();
                if (successPo != null) {
                    p.setStatus("PAID");
                    p.setPayTime(LocalDateTime.now());
                    participationMapper.updateById(p);
                    log.warn("[拼团自愈] JOINED→PAID 补偿标记: campaignId={}, orderId={}, participationId={}",
                            campaign.getId(), p.getOrderId(), p.getId());
                }
            } catch (Exception e) {
                log.warn("[拼团自愈] 补偿 JOINED→PAID 失败，跳过: campaignId={}, orderId={}, err={}",
                        campaign.getId(), p.getOrderId(), e.getMessage());
            }
        }
    }
}
