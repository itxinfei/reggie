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
     * 标记拼团参与已支付（JOINED → PAID）
     * 供订单支付成功后调用，打通 PAID 状态流转
     */
    void markParticipationPaid(Long orderId);

    /**
     * 定时关闭过期拼团活动
     */
    int autoCloseExpiredCampaigns();

    /**
     * 成团判定并处理状态流转
     * <p>
     * 由定时任务调用，扫描所有 OPEN 状态且已结束未成团的 campaign：
     * <ul>
     *   <li>已付款参与人数 ≥ minMembers → 成团，标记 campaign=CLOSED（可履约）</li>
     *   <li>未成团 → 标记 campaign=ENDED，触发全额退款</li>
     * </ul>
     * </p>
     * @return 处理数量
     */
    int scanGroupFormedAndNotFormed();
}
