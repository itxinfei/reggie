package com.reggie.module.groupbuy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.groupbuy.model.GroupBuyCampaign;
import com.reggie.module.groupbuy.model.GroupBuyParticipation;

/**
 * <p>
 * 拼团活动服务接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
public interface GroupBuyService extends IService<GroupBuyCampaign> {

    /**
     * 创建拼团活动
     */
    GroupBuyCampaign createCampaign(GroupBuyCampaign campaign);

    /**
     * 更新拼团活动
     */
    GroupBuyCampaign updateCampaign(GroupBuyCampaign campaign);

    /**
     * 删除拼团活动
     */
    void deleteCampaign(Long id);

    /**
     * 分页查询拼团活动
     */
    Page<GroupBuyCampaign> listCampaigns(int page, int pageSize, String name);

    /**
     * 用户加入拼团
     */
    GroupBuyParticipation joinGroupBuy(Long campaignId, Long orderId, Long userId);

    /**
     * 检查拼团状态是否成团
     */
    boolean checkGroupBuyStatus(Long campaignId);

    /**
     * 定时关闭过期拼团活动
     */
    int autoCloseExpiredCampaigns();
}
