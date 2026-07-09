package com.reggie.module.recommend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.recommend.model.MarketingCampaign;
import com.reggie.module.recommend.service.MarketingCampaignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * 营销活动管理Controller
 * 提供营销活动的CRUD、推送、自动发券等API
 *
 * @author Reggie Team
 */
@Slf4j
@RestController
@RequestMapping("/marketing")
public class MarketingController {

    @Autowired
    private MarketingCampaignService marketingCampaignService;

    /**
     * 分页查询营销活动
     * GET /marketing/campaigns/page?page=1&pageSize=10&name=&status=
     */
    @GetMapping("/campaigns/page")
    public R<Page<MarketingCampaign>> pageCampaigns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        Page<MarketingCampaign> result = marketingCampaignService.pageCampaigns(page, pageSize, name, status);
        return R.success(result);
    }

    /**
     * 新增营销活动
     * POST /marketing/campaigns
     */
    @PostMapping("/campaigns")
    public R<MarketingCampaign> createCampaign(@Valid @RequestBody MarketingCampaign campaign) {
        campaign.setStatus(MarketingCampaign.STATUS_DRAFT);
        campaign.setCurrentParticipants(0);
        marketingCampaignService.save(campaign);
        log.info("[营销管理] 创建活动: {}", campaign.getName());
        return R.success(campaign);
    }

    /**
     * 修改营销活动
     * PUT /marketing/campaigns
     */
    @PutMapping("/campaigns")
    public R<MarketingCampaign> updateCampaign(@Valid @RequestBody MarketingCampaign campaign) {
        marketingCampaignService.updateById(campaign);
        log.info("[营销管理] 更新活动: id={}", campaign.getId());
        return R.success(campaign);
    }

    /**
     * 删除营销活动
     * DELETE /marketing/campaigns/{id}
     */
    @DeleteMapping("/campaigns/{id}")
    public R<String> deleteCampaign(@PathVariable Long id) {
        marketingCampaignService.removeById(id);
        log.info("[营销管理] 删除活动: id={}", id);
        return R.success("删除成功");
    }

    /**
     * 查询单个营销活动
     * GET /marketing/campaigns/{id}
     */
    @GetMapping("/campaigns/{id}")
    public R<MarketingCampaign> getCampaign(@PathVariable Long id) {
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        return R.success(campaign);
    }

    /**
     * 发布活动（草稿 -> 进行中）
     * PUT /marketing/campaigns/{id}/publish
     */
    @PutMapping("/campaigns/{id}/publish")
    public R<String> publishCampaign(@PathVariable Long id) {
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        if (campaign == null) {
            return R.error("活动不存在");
        }
        campaign.setStatus(MarketingCampaign.STATUS_ACTIVE);
        marketingCampaignService.updateById(campaign);
        log.info("[营销管理] 发布活动: {}", campaign.getName());
        return R.success("活动已发布");
    }

    /**
     * 暂停/结束活动
     * PUT /marketing/campaigns/{id}/pause
     */
    @PutMapping("/campaigns/{id}/pause")
    public R<String> pauseCampaign(@PathVariable Long id) {
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        if (campaign == null) {
            return R.error("活动不存在");
        }
        campaign.setStatus(MarketingCampaign.STATUS_PAUSED);
        marketingCampaignService.updateById(campaign);
        return R.success("活动已暂停");
    }

    /**
     * 为目标用户推送营销消息
     * POST /marketing/push/{campaignId}/{userId}
     */
    @PostMapping("/push/{campaignId}/{userId}")
    public R<String> pushMessage(@PathVariable Long campaignId,
                                  @PathVariable Long userId,
                                  @RequestParam(defaultValue = "1") Integer pushType) {
        boolean success = marketingCampaignService.pushMarketingMessage(campaignId, userId, pushType);
        return success ? R.success("推送成功") : R.error("推送失败");
    }

    /**
     * 自动为当前用户发券
     * POST /marketing/auto-dispatch-coupons
     */
    @PostMapping("/auto-dispatch-coupons")
    public R<String> autoDispatchCoupons(@RequestParam Long userId) {
        int count = marketingCampaignService.autoDispatchCoupons(userId);
        return R.success("已发放" + count + "张优惠券");
    }

    /**
     * 获取推送预览 - 查看目标用户预览列表
     * GET /marketing/push-preview/{campaignId}?limit=10
     */
    @GetMapping("/push-preview/{campaignId}")
    public R<Map<String, Object>> pushPreview(@PathVariable Long campaignId,
                                               @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        // 模拟预览数据
        List<Map<String, Object>> preview = new ArrayList<>();
        String[] mockNames = {"张*三", "李*四", "王*五", "赵*六", "陈*七", "刘*八", "周*九", "吴*十", "郑*一", "冯*二"};
        String[] mockReasons = {"新用户", "高频消费", "近期浏览", "活跃用户", "流失预警"};
        for (int i = 0; i < Math.min(limit, 10); i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", (long) (1000 + i));
            user.put("name", mockNames[i % mockNames.length]);
            user.put("matchReason", mockReasons[i % mockReasons.length]);
            preview.add(user);
        }
        result.put("preview", preview);
        result.put("estimate", 150 + (int) (Math.random() * 300));
        return R.success(result);
    }

    /**
     * 修改点：批量推送 - 根据活动目标人群自动匹配用户并推送
     * POST /marketing/batch-push/{campaignId}
     * 请求体: {"pushType": 1}
     */
    @PostMapping("/batch-push/{campaignId}")
    public R<String> batchPush(@PathVariable Long campaignId,
                                @RequestBody Map<String, Object> body) {
        Integer pushType = body.get("pushType") != null ?
                Integer.valueOf(body.get("pushType").toString()) : 1;
        int count = marketingCampaignService.batchPushMessages(campaignId, pushType);
        return R.success("已向" + count + "位用户推送营销消息");
    }
}
