package com.reggie.module.marketing.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.marketing.dto.BatchDeleteCampaignsDTO;
import com.reggie.module.marketing.dto.BatchPushCampaignDTO;
import com.reggie.module.marketing.model.MarketingCampaign;
import com.reggie.module.marketing.service.MarketingCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 营销活动管理控制器
 * 提供营销活动的CRUD、推送、自动发券等接口
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/marketing")
@Tag(name = "营销管理", description = "营销活动CRUD、推送、自动发券等接口")
@RequireEmployee
public class MarketingCampaignController {

    @Autowired
    private MarketingCampaignService marketingCampaignService;

    /**
     * 分页查询营销活动列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 活动名称（可选）
     * @param status 状态（可选）
     * @param campaignType 活动类型（可选）
     * @return 分页结果
     */
    @GetMapping("/campaigns/page")
    @Operation(summary = "分页查询营销活动", description = "分页查询营销活动列表，支持按名称、状态、活动类型筛选")
    public R<Page<MarketingCampaign>> pageCampaigns(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "活动名称（可选）") @RequestParam(required = false) String name,
            @Parameter(description = "状态（可选）") @RequestParam(required = false) Integer status,
            @Parameter(description = "活动类型（可选）") @RequestParam(required = false) Integer campaignType) {
        Page<MarketingCampaign> result = marketingCampaignService.pageCampaigns(page, PageUtils.cap(pageSize), name, status, campaignType);
        return R.success(result);
    }

    /**
     * 创建新的营销活动
     * <p>租户安全：强制设置 tenantId；仅保留业务字段。</p>
     *
     * @param campaign 营销活动信息
     * @return 创建的活动
     */
    @PostMapping("/campaigns")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "创建营销活动", description = "创建新的营销活动，初始状态为草稿")
    public R<MarketingCampaign> createCampaign(
            @Parameter(description = "营销活动信息", required = true) @Valid @RequestBody MarketingCampaign campaign) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        campaign.setTenantId(tenantId);
        campaign.setStatus(MarketingCampaign.STATUS_DRAFT);
        campaign.setCurrentParticipants(0);
        marketingCampaignService.save(campaign);
        log.info("[营销管理] 创建活动: {}", campaign.getName());
        return R.success(campaign);
    }

    /**
     * 更新营销活动
     * <p>租户安全：先校验归属，再更新业务字段。</p>
     *
     * @param campaign 营销活动信息
     * @return 更新后的活动
     */
    @PutMapping("/campaigns")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新营销活动", description = "更新营销活动信息")
    public R<MarketingCampaign> updateCampaign(
            @Parameter(description = "营销活动信息", required = true) @Valid @RequestBody MarketingCampaign campaign) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MarketingCampaign exist = marketingCampaignService.getById(campaign.getId());
        if (exist == null) {
            throw new CustomException("营销活动不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("营销活动不属于当前租户");
        }
        exist.setName(campaign.getName());
        exist.setDescription(campaign.getDescription());
        exist.setCampaignType(campaign.getCampaignType());
        exist.setTargetType(campaign.getTargetType());
        exist.setTargetValue(campaign.getTargetValue());
        exist.setRuleJson(campaign.getRuleJson());
        exist.setStatus(campaign.getStatus());
        exist.setPriority(campaign.getPriority());
        exist.setStartTime(campaign.getStartTime());
        exist.setEndTime(campaign.getEndTime());
        exist.setMaxParticipants(campaign.getMaxParticipants());
        exist.setCouponTemplateId(campaign.getCouponTemplateId());
        marketingCampaignService.updateById(exist);
        log.info("[营销管理] 更新活动: id={}", campaign.getId());
        return R.success(exist);
    }

    /**
     * 批量删除营销活动
     * <p>租户安全：每条记录先校验租户归属再删除。</p>
     *
     * @param body 活动ID列表
     * @return 操作结果
     */
    @PostMapping("/campaigns/batch-delete")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量删除营销活动", description = "批量删除指定的营销活动")
    public R<String> batchDeleteCampaigns(
            @Parameter(description = "活动ID列表", required = true) @Valid @RequestBody BatchDeleteCampaignsDTO dto) {
        List<Long> ids = dto.getIds();
        if (ids == null || ids.isEmpty()) {
            return R.error("请选择要删除的活动");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        int deleted = 0;
        for (Long id : ids) {
            MarketingCampaign campaign = marketingCampaignService.getById(id);
            if (campaign == null) {
                continue;
            }
            if (!tenantId.equals(campaign.getTenantId())) {
                throw new CustomException("营销活动中存在不属于当前租户的记录");
            }
            marketingCampaignService.removeById(id);
            deleted++;
        }
        log.info("[营销管理] 批量删除活动: count={}", deleted);
        return R.success("成功删除 " + deleted + " 个活动");
    }

    /**
     * 删除指定营销活动
     * <p>租户安全：先校验归属。</p>
     *
     * @param id 活动ID
     * @return 操作结果
     */
    @DeleteMapping("/campaigns/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除营销活动", description = "删除指定的营销活动")
    public R<String> deleteCampaign(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        if (campaign == null) {
            throw new CustomException("营销活动不存在");
        }
        if (!tenantId.equals(campaign.getTenantId())) {
            throw new CustomException("营销活动不属于当前租户");
        }
        marketingCampaignService.removeById(id);
        log.info("[营销管理] 删除活动: id={}", id);
        return R.success("删除成功");
    }

    /**
     * 查询单个营销活动
     * @param id 活动ID
     * @return 活动详情
     */
    @GetMapping("/campaigns/{id}")
    @Operation(summary = "查询营销活动", description = "根据ID查询单个营销活动详情")
    public R<MarketingCampaign> getCampaign(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        return R.success(campaign);
    }

    /**
     * 发布活动（草稿转为进行中）
     * <p>租户安全：先校验归属。</p>
     *
     * @param id 活动ID
     * @return 操作结果
     */
    @PutMapping("/campaigns/{id}/publish")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "发布营销活动", description = "将草稿状态的活动发布为进行中状态")
    public R<String> publishCampaign(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        if (campaign == null) {
            return R.error("活动不存在");
        }
        if (!tenantId.equals(campaign.getTenantId())) {
            throw new CustomException("营销活动不属于当前租户");
        }
        campaign.setStatus(MarketingCampaign.STATUS_ACTIVE);
        marketingCampaignService.updateById(campaign);
        log.info("[营销管理] 发布活动: {}", campaign.getName());
        return R.success("活动已发布");
    }

    /**
     * 暂停/结束营销活动
     * <p>租户安全：先校验归属。</p>
     *
     * @param id 活动ID
     * @return 操作结果
     */
    @PutMapping("/campaigns/{id}/pause")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "暂停营销活动", description = "暂停（结束）指定的营销活动")
    public R<String> pauseCampaign(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MarketingCampaign campaign = marketingCampaignService.getById(id);
        if (campaign == null) {
            return R.error("活动不存在");
        }
        if (!tenantId.equals(campaign.getTenantId())) {
            throw new CustomException("营销活动不属于当前租户");
        }
        campaign.setStatus(MarketingCampaign.STATUS_PAUSED);
        marketingCampaignService.updateById(campaign);
        return R.success("活动已暂停");
    }

    /**
     * 向指定用户推送营销消息
     * @param campaignId 活动ID
     * @param userId 用户ID
     * @param pushType 推送类型
     * @return 操作结果
     */
    @PostMapping("/push/{campaignId}/{userId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "推送营销消息", description = "向指定用户推送营销活动消息")
    public R<String> pushMessage(
            @Parameter(description = "活动ID", required = true) @PathVariable Long campaignId,
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "推送类型") @RequestParam(defaultValue = "1") Integer pushType) {
        boolean success = marketingCampaignService.pushMarketingMessage(campaignId, userId, pushType);
        return success ? R.success("推送成功") : R.error("推送失败");
    }

    /**
     * 根据用户画像自动发放匹配的优惠券
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/auto-dispatch-coupons")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "自动发放优惠券", description = "根据用户画像自动为当前用户发放匹配的优惠券")
    public R<String> autoDispatchCoupons(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        int count = marketingCampaignService.autoDispatchCoupons(userId);
        return R.success("已发放" + count + "张优惠券");
    }

    /**
     * 查看匹配该活动的真实用户列表预览
     * @param campaignId 活动ID
     * @param limit 预览数量
     * @return 匹配用户预览
     */
    @GetMapping("/push-preview/{campaignId}")
    @Operation(summary = "推送预览", description = "查看匹配该活动的真实用户列表预览")
    public R<Map<String, Object>> pushPreview(
            @Parameter(description = "活动ID", required = true) @PathVariable Long campaignId,
            @Parameter(description = "预览数量") @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = marketingCampaignService.getPushPreview(campaignId, limit);
        return R.success(result);
    }

    /**
     * 根据活动目标人群自动匹配用户并批量推送营销消息
     * @param campaignId 活动ID
     * @param body 推送参数（pushType）
     * @return 推送结果
     */
    @PostMapping("/batch-push/{campaignId}")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量推送营销消息", description = "根据活动目标人群自动匹配用户并批量推送营销消息")
    public R<String> batchPush(
            @Parameter(description = "活动ID", required = true) @PathVariable Long campaignId,
            @Parameter(description = "推送参数（pushType）") @Valid @RequestBody BatchPushCampaignDTO dto) {
        Integer pushType = dto.getPushType();
        int count = marketingCampaignService.batchPushMessages(campaignId, pushType);
        return R.success("已向" + count + "位用户推送营销消息");
    }

    /**
     * 获取所有营销活动名称，供搜索条件下拉框使用
     * @return 营销活动名称列表
     */
    @GetMapping("/campaigns/options")
    @Operation(summary = "筛选选项", description = "获取所有营销活动名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> campaignOptions() {
        List<MarketingCampaign> list = marketingCampaignService.list();
        Set<String> nameSet = new HashSet<>();
        for (MarketingCampaign c : list) {
            if (c.getName() != null && !c.getName().isEmpty()) { nameSet.add(c.getName()); }
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }

    /**
     * 获取营销活动全局统计数据
     * @return 营销活动全局统计
     */
    @GetMapping("/campaigns/stats")
    @Operation(summary = "营销活动统计", description = "获取营销活动全局统计数据")
    public R<Map<String, Object>> campaignStats() {
        Map<String, Object> stats = marketingCampaignService.getCampaignStats();
        return R.success(stats);
    }

    /**
     * 获取指定营销活动的推送次数统计
     * @param id 活动ID
     * @return 推送次数
     */
    @GetMapping("/campaigns/{id}/push-count")
    @Operation(summary = "活动推送次数", description = "获取指定营销活动的推送次数统计")
    public R<Integer> pushCount(
            @Parameter(description = "活动ID", required = true) @PathVariable Long id) {
        int count = marketingCampaignService.getPushCountByCampaignId(id);
        return R.success(count);
    }
}




