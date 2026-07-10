package com.reggie.module.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.entity.User;
import com.reggie.mapper.UserMapper;
import com.reggie.module.member.mapper.CouponTemplateMapper;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.recommend.mapper.MarketingCampaignMapper;
import com.reggie.module.recommend.mapper.MarketingMessageMapper;
import com.reggie.module.recommend.model.MarketingCampaign;
import com.reggie.module.recommend.model.MarketingMessage;
import com.reggie.module.recommend.service.MarketingCampaignService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现
 * 实现用户画像匹配、智能推送和自动发券
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class MarketingCampaignServiceImpl extends ServiceImpl<MarketingCampaignMapper, MarketingCampaign>
        implements MarketingCampaignService {

    /** 营销活动Mapper */
    @Autowired
    private MarketingCampaignMapper campaignMapper;
    /** 营销消息Mapper */
    @Autowired
    private MarketingMessageMapper messageMapper;
    /** 优惠券模板服务 */
    @Autowired
    private CouponTemplateService couponTemplateService;
    /** 优惠券模板Mapper */
    @Autowired
    private CouponTemplateMapper couponTemplateMapper;
    /** 用户优惠券Mapper */
    @Autowired
    private CouponUserMapper couponUserMapper;
    /** 用户偏好分析服务 */
    @Autowired
    private PreferenceAnalysisService preferenceAnalysisService;
    /** 用户Mapper */
    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<MarketingCampaign> pageCampaigns(int page, int pageSize, String name, Integer status, Integer campaignType) {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<MarketingCampaign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, MarketingCampaign::getTenantId, tenantId);
        if (name != null && !name.isEmpty()) {
            wrapper.like(MarketingCampaign::getName, name);
        }
        if (status != null) {
            wrapper.eq(MarketingCampaign::getStatus, status);
        }
        if (campaignType != null) {
            wrapper.eq(MarketingCampaign::getCampaignType, campaignType);
        }
        wrapper.orderByDesc(MarketingCampaign::getPriority)
               .orderByDesc(MarketingCampaign::getCreateTime);

        return campaignMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public int batchDeleteCampaigns(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = campaignMapper.deleteBatchIds(ids);
        log.info("[营销管理] 批量删除活动: ids={}, count={}", ids, count);
        return count;
    }

    @Override
    public List<MarketingCampaign> matchCampaignsForUser(Long userId) {
        if (userId == null) return Collections.emptyList();
        Long tenantId = BaseContext.getCurrentTenantId();

        // 查询当前生效的营销活动
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<MarketingCampaign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, MarketingCampaign::getTenantId, tenantId)
               .eq(MarketingCampaign::getStatus, MarketingCampaign.STATUS_ACTIVE)
               .le(MarketingCampaign::getStartTime, now)
               .ge(MarketingCampaign::getEndTime, now)
               .orderByDesc(MarketingCampaign::getPriority);

        List<MarketingCampaign> activeCampaigns = campaignMapper.selectList(wrapper);

        // 根据用户画像匹配
        boolean isNewUser = isNewUser(userId);
        boolean isHighFreq = preferenceAnalysisService.isHighFrequencyUser(userId);
        boolean isChurnWarning = preferenceAnalysisService.isChurnWarningUser(userId);

        return activeCampaigns.stream()
                .filter(c -> isUserMatchCampaign(c, isNewUser, isHighFreq, isChurnWarning))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean pushMarketingMessage(Long campaignId, Long userId, Integer pushType) {
        if (campaignId == null || userId == null) return false;

        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            log.warn("[营销推送] 活动不存在: {}", campaignId);
            return false;
        }

        // 检查参与人数上限
        if (campaign.getMaxParticipants() != null &&
            campaign.getCurrentParticipants() >= campaign.getMaxParticipants()) {
            log.info("[营销推送] 活动{}已达参与上限", campaignId);
            return false;
        }

        MarketingMessage message = new MarketingMessage();
        message.setCampaignId(campaignId);
        message.setUserId(userId);
        message.setPushType(pushType != null ? pushType : MarketingMessage.PUSH_POPUP);
        message.setTitle(campaign.getName());
        message.setContent(campaign.getDescription() != null ?
                campaign.getDescription() : "您有一份专属优惠待领取！");
        message.setStatus(MarketingMessage.STATUS_SENT);

        messageMapper.insert(message);

        // 更新参与人数
        campaign.setCurrentParticipants(campaign.getCurrentParticipants() + 1);
        updateById(campaign);

        log.info("[营销推送] 活动{}推送至用户{}, 状态=SENT", campaignId, userId);
        return true;
    }

    @Override
    public List<Map<String, Object>> getUnreadMessages(Long userId) {
        if (userId == null) return Collections.emptyList();

        LambdaQueryWrapper<MarketingMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMessage::getUserId, userId)
               .eq(MarketingMessage::getStatus, MarketingMessage.STATUS_SENT)
               .orderByDesc(MarketingMessage::getCreateTime);

        return messageMapper.selectList(wrapper).stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("campaignId", m.getCampaignId());
            map.put("title", m.getTitle());
            map.put("content", m.getContent());
            map.put("pushType", m.getPushType());
            map.put("createTime", m.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public void markMessageRead(Long messageId) {
        if (messageId == null) return;
        MarketingMessage message = messageMapper.selectById(messageId);
        if (message != null) {
            message.setStatus(MarketingMessage.STATUS_READ);
            message.setReadTime(LocalDateTime.now());
            messageMapper.updateById(message);
        }
    }

    @Override
    public Page<Map<String, Object>> getMessages(Long userId, int page, int pageSize) {
        if (userId == null) return new Page<>();

        LambdaQueryWrapper<MarketingMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMessage::getUserId, userId)
               .in(MarketingMessage::getStatus, MarketingMessage.STATUS_SENT,
                       MarketingMessage.STATUS_READ, MarketingMessage.STATUS_USED)
               .orderByDesc(MarketingMessage::getCreateTime);

        Page<MarketingMessage> msgPage = messageMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Page<Map<String, Object>> resultPage = new Page<>(page, pageSize, msgPage.getTotal());
        resultPage.setRecords(msgPage.getRecords().stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("content", m.getContent());
            map.put("pushType", m.getPushType());
            map.put("status", m.getStatus());
            map.put("readTime", m.getReadTime());
            map.put("createTime", m.getCreateTime());
            return map;
        }).collect(Collectors.toList()));
        return resultPage;
    }

    @Override
    public int getUnreadCount(Long userId) {
        if (userId == null) return 0;
        LambdaQueryWrapper<MarketingMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMessage::getUserId, userId)
               .eq(MarketingMessage::getStatus, MarketingMessage.STATUS_SENT);
        return (int) messageMapper.selectCount(wrapper);
    }

    @Override
    @Transactional
    public int autoDispatchCoupons(Long userId) {
        if (userId == null) return 0;
        Long tenantId = BaseContext.getCurrentTenantId();

        // 匹配用户适合的营销活动
        List<MarketingCampaign> campaigns = matchCampaignsForUser(userId);
        int dispatched = 0;

        for (MarketingCampaign campaign : campaigns) {
            if (campaign.getCouponTemplateId() == null) continue;

            // 检查是否已发过
            boolean alreadyReceived = checkAlreadyReceived(userId, campaign.getCouponTemplateId());
            if (alreadyReceived) continue;

            // 自动发放优惠券
            try {
                couponTemplateService.claimCoupon(userId, campaign.getCouponTemplateId());

                // 记录推送消息
                MarketingMessage message = new MarketingMessage();
                message.setCampaignId(campaign.getId());
                message.setUserId(userId);
                message.setPushType(MarketingMessage.PUSH_COUPON);
                message.setTitle("您有一张优惠券");
                message.setContent(campaign.getName() + " - 自动赠送，快去使用吧！");
                message.setStatus(MarketingMessage.STATUS_SENT);
                messageMapper.insert(message);

                dispatched++;
            } catch (Exception e) {
                log.warn("[自动发券] 活动{}发放失败: {}", campaign.getId(), e.getMessage());
            }
        }

        log.info("[自动发券] 为用户{}自动发放了{}张优惠券", userId, dispatched);
        return dispatched;
    }

    // ==================== 私有方法 ====================

    /**
     * 判断用户是否匹配活动目标人群
     */
    private boolean isUserMatchCampaign(MarketingCampaign campaign, boolean isNewUser,
                                         boolean isHighFreq, boolean isChurnWarning) {
        switch (campaign.getTargetType()) {
            case MarketingCampaign.TARGET_ALL: return true;
            case MarketingCampaign.TARGET_NEW_USER: return isNewUser;
            case MarketingCampaign.TARGET_HIGH_FREQ: return isHighFreq;
            case MarketingCampaign.TARGET_CHURN_WARNING: return isChurnWarning;
            case MarketingCampaign.TARGET_SPECIFIC_LEVEL:
                // 需具体等级匹配（预留扩展）
                return true;
            default: return false;
        }
    }

    /**
     * 判断用户是否为新用户（注册7天内且订单<3单）
     */
    private boolean isNewUser(Long userId) {
        // 简化判断：通过优惠券领取记录判断
        LambdaQueryWrapper<CouponUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponUser::getMemberId, userId);
        return couponUserMapper.selectCount(wrapper) <= 1;
    }

    @Override
    @Transactional
    public int batchPushMessages(Long campaignId, Integer pushType) {
        MarketingCampaign campaign = getById(campaignId);
        if (campaign == null) {
            log.warn("[批量推送] 活动不存在: {}", campaignId);
            return 0;
        }

        Long tenantId = BaseContext.getCurrentTenantId();

        // 查询当前门店所有用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(tenantId != null, User::getTenantId, tenantId)
                   .eq(User::getStatus, 1); // 启用状态
        List<User> allUsers = userMapper.selectList(userWrapper);

        int pushed = 0;
        for (User user : allUsers) {
            try {
                // 检查该用户是否匹配活动目标人群
                boolean isNewUser = isNewUser(user.getId());
                boolean isHighFreq = preferenceAnalysisService.isHighFrequencyUser(user.getId());
                boolean isChurnWarning = preferenceAnalysisService.isChurnWarningUser(user.getId());

                if (!isUserMatchCampaign(campaign, isNewUser, isHighFreq, isChurnWarning)) {
                    continue;
                }

                // 检查参与上限
                if (campaign.getMaxParticipants() != null &&
                        campaign.getCurrentParticipants() >= campaign.getMaxParticipants()) {
                    break;
                }

                // 创建推送消息
                MarketingMessage message = new MarketingMessage();
                message.setCampaignId(campaignId);
                message.setUserId(user.getId());
                message.setPushType(pushType != null ? pushType : MarketingMessage.PUSH_POPUP);
                message.setTitle(campaign.getName());
                message.setContent(campaign.getDescription() != null ?
                        campaign.getDescription() : "您有一份专属优惠待领取！");
                message.setStatus(MarketingMessage.STATUS_SENT); // 推送即已发送
                messageMapper.insert(message);
                pushed++;
            } catch (Exception e) {
                log.warn("[批量推送] 用户{}推送失败: {}", user.getId(), e.getMessage());
            }
        }

        // 更新参与人数
        campaign.setCurrentParticipants(campaign.getCurrentParticipants() + pushed);
        updateById(campaign);

        log.info("[批量推送] 活动{}批量推送完成：推送{}/{}人", campaignId, pushed, allUsers.size());
        return pushed;
    }

    /**
     * 检查用户是否已领取过该优惠券模板
     */
    private boolean checkAlreadyReceived(Long userId, Long templateId) {
        LambdaQueryWrapper<CouponUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponUser::getMemberId, userId)
               .eq(CouponUser::getTemplateId, templateId);
        return couponUserMapper.selectCount(wrapper) > 0;
    }
}
