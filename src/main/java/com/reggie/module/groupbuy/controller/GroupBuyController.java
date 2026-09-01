package com.reggie.module.groupbuy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.groupbuy.model.GroupBuyCampaign;
import com.reggie.module.groupbuy.model.GroupBuyParticipation;
import com.reggie.module.groupbuy.service.GroupBuyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 拼团活动管理控制器
 *
 * @author reggie
 * @since 2026-09-01
 */
@RequireEmployee
@RestController
@RequestMapping("/api/groupbuy/campaign")
@Tag(name = "拼团活动管理")
public class GroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    @PostMapping
    @Operation(summary = "创建拼团活动")
    public R<GroupBuyCampaign> create(@Parameter(description = "拼团活动信息", required = true) @RequestBody GroupBuyCampaign campaign) {
        return R.success(groupBuyService.createCampaign(campaign));
    }

    @PutMapping
    @Operation(summary = "更新拼团活动")
    public R<GroupBuyCampaign> update(@Parameter(description = "拼团活动信息", required = true) @RequestBody GroupBuyCampaign campaign) {
        return R.success(groupBuyService.updateCampaign(campaign));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除拼团活动")
    @Parameter(name = "id", description = "拼团活动ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        groupBuyService.deleteCampaign(id);
        return R.success("删除成功");
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询拼团活动")
    public R<Page<GroupBuyCampaign>> page(
            @Parameter(description = "页码，从1开始", required = true) @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，最大100", required = true) @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "活动名称，模糊筛选") @RequestParam(required = false) String name) {
        return R.success(groupBuyService.listCampaigns(page, pageSize, name));
    }

    @PostMapping("/{campaignId}/join")
    @Operation(summary = "用户加入拼团")
    @Parameter(name = "campaignId", description = "拼团活动ID", required = true)
    public R<GroupBuyParticipation> join(
            @PathVariable Long campaignId,
            @Parameter(description = "参与拼团的订单ID", required = true) @RequestParam Long orderId,
            @Parameter(description = "参与用户ID", required = true) @RequestParam Long userId) {
        return R.success(groupBuyService.joinGroupBuy(campaignId, orderId, userId));
    }

    @GetMapping("/{campaignId}/check")
    @Operation(summary = "检查拼团是否成团")
    @Parameter(name = "campaignId", description = "拼团活动ID", required = true)
    public R<Map<String, Object>> check(@PathVariable Long campaignId) {
        boolean enough = groupBuyService.checkGroupBuyStatus(campaignId);
        Map<String, Object> result = new HashMap<>();
        result.put("campaignId", campaignId);
        result.put("enough", enough);
        return R.success(result);
    }

    @PostMapping("/participation/{orderId}/pay")
    @Operation(summary = "标记拼团参与已支付")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    public R<String> markPaid(@PathVariable Long orderId) {
        groupBuyService.markParticipationPaid(orderId);
        return R.success("标记成功");
    }

    @PostMapping("/auto-close")
    @Operation(summary = "定时关闭过期拼团活动")
    public R<Integer> autoClose() {
        int closed = groupBuyService.autoCloseExpiredCampaigns();
        return R.success(closed);
    }
}
