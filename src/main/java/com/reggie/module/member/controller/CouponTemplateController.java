package com.reggie.module.member.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.BatchExtendCouponDTO;
import com.reggie.dto.ClaimCouponDTO;
import com.reggie.dto.IssueByConditionDTO;
import com.reggie.dto.IssueByMembersDTO;
import com.reggie.module.member.model.CouponEffectVO;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponTemplateSaveDTO;
import com.reggie.module.member.model.CouponTemplateUpdateDTO;
import com.reggie.module.member.model.ExpiringByTemplateVO;
import com.reggie.module.member.model.ExpiringCouponVO;
import com.reggie.module.member.model.IssuedMemberVO;
import com.reggie.module.member.service.CouponTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 优惠券模板管理控制器
 * 提供优惠券模板的增删改查、领取等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/member/coupon-template")
@Tag(name = "优惠券模板")
@RequireEmployee
public class CouponTemplateController {

    @Autowired
    private CouponTemplateService couponTemplateService;

    /**
     * 分页查询优惠券模板列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 优惠券名称（可选，模糊查询）
     * @param type 优惠券类型（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询优惠券模板列表，支持按名称、类型、状态筛选，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "优惠券名称（可选，模糊查询）")
    @Parameter(name = "type", description = "优惠券类型（可选，FULL_REDUCTION/DISCOUNT/NEW_MEMBER）")
    @Parameter(name = "status", description = "状态（可选，0禁用 1启用）")
    public R<Page<CouponTemplate>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize, String name, String type, Integer status) {
        Page<CouponTemplate> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), CouponTemplate::getName, name);
        qw.eq(type != null && !type.isEmpty(), CouponTemplate::getType, type);
        qw.eq(status != null, CouponTemplate::getStatus, status);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(CouponTemplate::getTenantId, tenantId);
        }
        qw.orderByAsc(CouponTemplate::getId);
        couponTemplateService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 新增优惠券模板
     * <p>租户安全：使用 CouponTemplateSaveDTO 仅接收业务字段，tenantId 由 Service 层通过 BaseContext 强制设置。</p>
     *
     * @param dto 优惠券模板信息
     * @return 操作结果
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增优惠券", description = "创建新的优惠券模板，校验名称/类型/金额合法性")
    public R<String> save(@Valid @RequestBody CouponTemplateSaveDTO dto) {
        CouponTemplate template = new CouponTemplate();
        template.setType(dto.getType());
        template.setConditionAmount(dto.getConditionAmount());
        template.setDiscountAmount(dto.getDiscountAmount());
        template.setDiscountRate(dto.getDiscountRate());
        template.setTotalCount(dto.getTotalCount());
        template.setRemainCount(dto.getRemainCount());
        template.setValidDays(dto.getValidDays());
        template.setStatus(dto.getStatus());
        validateCoupon(template);
        log.info("新增优惠券模板: {}", dto.getName());
        couponTemplateService.addTenantCouponTemplate(
                dto.getName(), dto.getType(),
                dto.getConditionAmount(), dto.getDiscountAmount(), dto.getDiscountRate(),
                dto.getTotalCount(), dto.getRemainCount(), dto.getValidDays(), dto.getStatus());
        return R.success("新增优惠券成功");
    }

    /**
     * 修改优惠券模板
     * <p>租户安全：使用 CouponTemplateUpdateDTO，Service 层先校验归属再更新业务字段。</p>
     *
     * @param dto 优惠券模板信息
     * @return 操作结果
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改优惠券", description = "更新优惠券模板信息，校验名称/类型/金额合法性")
    public R<String> update(@Valid @RequestBody CouponTemplateUpdateDTO dto) {
        CouponTemplate template = new CouponTemplate();
        template.setId(dto.getId());
        template.setType(dto.getType());
        template.setConditionAmount(dto.getConditionAmount());
        template.setDiscountAmount(dto.getDiscountAmount());
        template.setDiscountRate(dto.getDiscountRate());
        template.setTotalCount(dto.getTotalCount());
        template.setRemainCount(dto.getRemainCount());
        template.setValidDays(dto.getValidDays());
        template.setStatus(dto.getStatus());
        validateCoupon(template);
        log.info("修改优惠券模板: {}", dto.getId());
        couponTemplateService.updateTenantCouponTemplate(
                dto.getId(),
                dto.getName(), dto.getType(),
                dto.getConditionAmount(), dto.getDiscountAmount(), dto.getDiscountRate(),
                dto.getTotalCount(), dto.getRemainCount(), dto.getValidDays(), dto.getStatus());
        return R.success("修改优惠券成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除优惠券", description = "根据ID删除优惠券模板（先校验租户归属）")
    @Parameter(name = "id", description = "优惠券模板ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除优惠券模板: {}", id);
        couponTemplateService.deleteTenantCouponTemplate(id);
        return R.success("删除优惠券成功");
    }

    /**
     * 根据ID查询优惠券模板
     * @param id 优惠券模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询优惠券", description = "根据ID查询优惠券模板详情")
    @Parameter(name = "id", description = "优惠券模板ID", required = true)
    public R<CouponTemplate> getById(@PathVariable Long id) {
        CouponTemplate template = couponTemplateService.getById(id);
        if (template == null) {
            return R.error("没有查询到对应优惠券");
        }
        // 租户归属校验：禁止越权查看其他租户模板
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(template.getTenantId())) {
            log.warn("[优惠券模板] 跨租户越权拦截: id={}, templateTenant={}, curTenant={}", id, template.getTenantId(), currentTenantId);
            return R.error("无权查看其他租户的优惠券模板");
        }
        return R.success(template);
    }

    /**
     * 优惠券模板统计
     * @return 总数、启用/禁用/已领完数量、累计发放/领取数、使用率（后端聚合，避免前端拉全量）
     */
    @GetMapping("/stats")
    @Operation(summary = "优惠券统计", description = "统计优惠券模板总数、启用/禁用/已领完数量及领取使用率")
    public R<Map<String, Object>> stats() {
        return R.success(couponTemplateService.getStats());
    }

    /**
     * 会员领取优惠券
     * @param dto 领券请求
     * @return 操作结果
     */
    @PostMapping("/claim")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "领取优惠券", description = "会员领取优惠券模板")
    public R<String> claim(@Valid @RequestBody ClaimCouponDTO dto) {
        boolean ok = couponTemplateService.claimCoupon(dto.getMemberId(), dto.getTemplateId());
        if (ok) {
            return R.success("领取成功");
        }
        return R.error("领取失败，优惠券不可用或已领完");
    }

    /**
     * 批量定向发券（按会员ID列表发放）
     * @param dto 批量发券请求
     * @return 发放结果（成功数/失败数/已领过数/总数）
     */
    @PostMapping("/batch-issue")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量定向发券", description = "按会员ID列表批量发放优惠券，返回成功/失败/已领过的统计")
    public R<Map<String, Object>> batchIssue(@Valid @RequestBody IssueByMembersDTO dto) {
        log.info("批量定向发券: templateId={}, memberIds.size={}", dto.getTemplateId(), dto.getMemberIds().size());
        Map<String, Object> result = couponTemplateService.batchIssue(dto.getTemplateId(), dto.getMemberIds());
        return R.success(result);
    }

    /**
     * 条件定向发券（按条件筛选会员后发放）
     * @param dto 条件发券请求
     * @return 发放结果（成功数/失败数/已领过数/总数）
     */
    @PostMapping("/issue-by-condition")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "条件定向发券", description = "按条件筛选会员后批量发放优惠券，条件支持 levelId/minPoints/maxPoints/minConsumption/maxConsumption/newMemberDays")
    public R<Map<String, Object>> issueByCondition(@Valid @RequestBody IssueByConditionDTO dto) {
        log.info("条件定向发券: templateId={}, condition={}", dto.getTemplateId(), dto.getCondition());
        Map<String, Object> result = couponTemplateService.issueByCondition(dto.getTemplateId(), dto.getCondition());
        return R.success(result);
    }

    /**
     * 分页查询某模板的发放会员明细
     * @param templateId 优惠券模板ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 发放会员分页列表
     */
    @GetMapping("/{templateId}/issued")
    @Operation(summary = "投放明细", description = "分页查询某优惠券模板已发放会员列表，含会员信息、用券状态、领取与使用时间")
    @Parameter(name = "templateId", description = "优惠券模板ID", required = true)
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<IssuedMemberVO>> issuedMembers(
            @PathVariable Long templateId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        log.info("查询投放明细: templateId={}, page={}, pageSize={}", templateId, page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IssuedMemberVO> pageInfo = PageUtils.of(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<IssuedMemberVO> result = couponTemplateService.issuedMembers(pageInfo, templateId);
        return R.success(result);
    }

    /**
     * 查询某模板的投放效果聚合指标
     * @param templateId 优惠券模板ID
     * @return 投放效果 VO（发放率/使用率/活跃率/状态分布）
     */
    @GetMapping("/{templateId}/effect")
    @Operation(summary = "投放效果", description = "查询某优惠券模板的投放效果聚合指标：发放率/使用率/活跃率/状态分布")
    @Parameter(name = "templateId", description = "优惠券模板ID", required = true)
    public R<CouponEffectVO> effect(@PathVariable Long templateId) {
        log.info("查询投放效果: templateId={}", templateId);
        CouponEffectVO vo = couponTemplateService.effect(templateId);
        if (vo == null) {
            return R.error("未找到对应优惠券模板");
        }
        return R.success(vo);
    }

    /**
     * 查询即将到期优惠券明细（分页）
     * @param days      预警天数窗口（默认7天）
     * @param templateId 优惠券模板ID（可选）
     * @param phone      会员手机（可选，模糊查询）
     * @param page       页码
     * @param pageSize   每页数量
     * @return 即将到期优惠券分页列表
     */
    @GetMapping("/expiring")
    @Operation(summary = "即将到期预警", description = "查询即将到期的unused优惠券明细，支持按天数/模板/会员手机筛选")
    @Parameter(name = "days", description = "预警天数窗口", example = "7")
    @Parameter(name = "templateId", description = "优惠券模板ID（可选）")
    @Parameter(name = "phone", description = "会员手机（可选，模糊查询）")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO>> expiring(
            @RequestParam(defaultValue = "7") int days,
            Long templateId,
            String phone,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        log.info("查询即将到期预警: days={}, templateId={}, phone={}, page={}", days, templateId, phone != null ? LogMaskUtils.maskPhone(phone) : "", page);
        if (days <= 0 || days > 90) {
            return R.error("预警天数必须在1~90之间");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO> pageInfo = PageUtils.of(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO> result =
                couponTemplateService.expiringCoupons(pageInfo, days, templateId, phone);
        return R.success(result);
    }

    /**
     * 查询已过期优惠券明细（分页）
     */
    @GetMapping("/expired")
    @Operation(summary = "已过期明细", description = "查询已过期(expired)优惠券明细，支持按模板/会员手机筛选")
    @Parameter(name = "templateId", description = "优惠券模板ID（可选）")
    @Parameter(name = "phone", description = "会员手机（可选，模糊查询）")
    public R<com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO>> expired(
            Long templateId,
            String phone,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize) {
        log.info("查询已过期明细: templateId={}, phone={}, page={}", templateId, phone != null ? LogMaskUtils.maskPhone(phone) : "", page);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO> pageInfo = PageUtils.of(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ExpiringCouponVO> result =
                couponTemplateService.expiredCoupons(pageInfo, templateId, phone);
        return R.success(result);
    }

    /**
     * 优惠券到期预警统计
     * @param days 预警天数窗口（默认7天）
     * @return 按模板聚合的预警统计列表
     */
    @GetMapping("/expiring-stats")
    @Operation(summary = "到期预警统计", description = "按模板聚合即将到期与已过期的优惠券数量及优惠总额")
    @Parameter(name = "days", description = "预警天数窗口", example = "7")
    public R<Map<String, Object>> expiringStats(@RequestParam(defaultValue = "7") int days) {
        log.info("查询到期预警统计: days={}", days);
        if (days <= 0 || days > 90) {
            return R.error("预警天数必须在1~90之间");
        }
        List<ExpiringByTemplateVO> list = couponTemplateService.expiringStats(days);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("days", days);
        int totalExpiring = 0;
        int totalExpired = 0;
        java.math.BigDecimal totalExpiringAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal totalExpiredAmount = java.math.BigDecimal.ZERO;
        for (ExpiringByTemplateVO v : list) {
            totalExpiring += v.getExpiringCount() != null ? v.getExpiringCount() : 0;
            totalExpired += v.getExpiredCount() != null ? v.getExpiredCount() : 0;
            totalExpiringAmount = totalExpiringAmount.add(
                    v.getExpiringDiscountAmount() != null ? v.getExpiringDiscountAmount() : java.math.BigDecimal.ZERO);
            totalExpiredAmount = totalExpiredAmount.add(
                    v.getExpiredDiscountAmount() != null ? v.getExpiredDiscountAmount() : java.math.BigDecimal.ZERO);
        }
        result.put("totalExpiringCount", totalExpiring);
        result.put("totalExpiredCount", totalExpired);
        result.put("totalExpiringAmount", totalExpiringAmount);
        result.put("totalExpiredAmount", totalExpiredAmount);
        result.put("byTemplate", list);
        return R.success(result);
    }

    /**
     * 批量延期优惠券
     * @param dto 批量延期请求
     * @return 处理结果（成功数/无效数/总数）
     */
    @PostMapping("/batch-extend")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量延期", description = "批量延长即将到期优惠券的过期时间（仅限unused状态）")
    public R<Map<String, Object>> batchExtend(@Valid @RequestBody BatchExtendCouponDTO dto) {
        log.info("批量延期优惠券: couponUserIds.size={}, extendDays={}",
                dto.getCouponUserIds().size(), dto.getExtendDays());
        Map<String, Object> result = couponTemplateService.batchExtend(
                dto.getCouponUserIds(), dto.getExtendDays());
        return R.success(result);
    }

    /**
     * 修改点：补充后端校验，防止空名称、非法类型、金额/折扣率不合法的数据入库
     * @param t 优惠券模板
     */
    private void validateCoupon(CouponTemplate t) {
        String type = t.getType();
        if (!"FULL_REDUCTION".equals(type) && !"DISCOUNT".equals(type) && !"NEW_MEMBER".equals(type)) {
            throw new CustomException("优惠券类型非法，仅支持 FULL_REDUCTION/DISCOUNT/NEW_MEMBER");
        }
        if ("FULL_REDUCTION".equals(type)) {
            if (t.getConditionAmount() == null || t.getConditionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException("满减券的条件金额必须大于0");
            }
            if (t.getDiscountAmount() == null || t.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CustomException("满减券的优惠金额必须大于0");
            }
            if (t.getDiscountAmount().compareTo(t.getConditionAmount()) >= 0) {
                throw new CustomException("满减券的优惠金额必须小于条件金额");
            }
        } else if ("DISCOUNT".equals(type)) {
            if (t.getDiscountRate() == null || t.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                    || t.getDiscountRate().compareTo(BigDecimal.ONE) >= 0) {
                throw new CustomException("折扣券的折扣率必须在0~1之间（如0.85表示8.5折）");
            }
        }
    }
}

