package com.reggie.module.recommend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.recommend.model.MarketingCampaign;

import java.util.List;
import java.util.Map;

/**
 * 营销活动服务
 */
public interface MarketingCampaignService extends IService<MarketingCampaign> {

    /**
     * 分页查询当前门店的营销活动
     *
     * @param page   分页参数
     * @param name   活动名称（模糊搜索）
     * @param status 状态筛选
     * @return 分页结果
     */
    Page<MarketingCampaign> pageCampaigns(int page, int pageSize, String name, Integer status);

    /**
     * 获取匹配用户的营销活动列表
     * 根据用户画像（新用户/高频用户/流失预警等）自动匹配推送
     *
     * @param userId 用户ID
     * @return 匹配的营销活动列表（按优先级排序）
     */
    List<MarketingCampaign> matchCampaignsForUser(Long userId);

    /**
     * 为指定用户推送营销消息
     *
     * @param campaignId 活动ID
     * @param userId     用户ID
     * @param pushType   推送类型
     * @return 推送结果
     */
    boolean pushMarketingMessage(Long campaignId, Long userId, Integer pushType);

    /**
     * 获取用户未读的营销消息
     *
     * @param userId 用户ID
     * @return 未读消息列表
     */
    List<Map<String, Object>> getUnreadMessages(Long userId);

    /**
     * 标记消息为已读
     *
     * @param messageId 消息ID
     */
    void markMessageRead(Long messageId);

    /**
     * 检测并自动为符合条件的用户发券
     *
     * @param userId 用户ID
     * @return 发放的优惠券数量
     */
    int autoDispatchCoupons(Long userId);
}
