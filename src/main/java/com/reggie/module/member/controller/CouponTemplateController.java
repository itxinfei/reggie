package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.dto.ClaimCouponDTO;
import com.reggie.module.member.model.CouponTemplate;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
    public R<Page<CouponTemplate>> page(int page, int pageSize, String name, String type, Integer status) {
        Page<CouponTemplate> pageInfo = new Page<>(page, pageSize);
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
     * @param couponTemplate 优惠券模板信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增优惠券", description = "创建新的优惠券模板，校验名称/类型/金额合法性")
    public R<String> save(@Valid @RequestBody CouponTemplate couponTemplate) {
        validateCoupon(couponTemplate);
        log.info("新增优惠券模板: {}", couponTemplate.getName());
        couponTemplate.setCreatedTime(LocalDateTime.now());
        couponTemplate.setUpdateTime(LocalDateTime.now());
        couponTemplateService.save(couponTemplate);
        return R.success("新增优惠券成功");
    }

    /**
     * 修改优惠券模板
     * @param couponTemplate 优惠券模板信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改优惠券", description = "更新优惠券模板信息，校验名称/类型/金额合法性")
    public R<String> update(@Valid @RequestBody CouponTemplate couponTemplate) {
        validateCoupon(couponTemplate);
        log.info("修改优惠券模板: {}", couponTemplate.getId());
        couponTemplate.setUpdateTime(LocalDateTime.now());
        couponTemplateService.updateById(couponTemplate);
        return R.success("修改优惠券成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除优惠券", description = "根据ID删除优惠券模板")
    @Parameter(name = "id", description = "优惠券模板ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除优惠券模板: {}", id);
        couponTemplateService.removeById(id);
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
        if (template != null) {
            return R.success(template);
        }
        return R.error("没有查询到对应优惠券");
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
    @Operation(summary = "领取优惠券", description = "会员领取优惠券模板")
    public R<String> claim(@Valid @RequestBody ClaimCouponDTO dto) {
        boolean ok = couponTemplateService.claimCoupon(dto.getMemberId(), dto.getTemplateId());
        if (ok) {
            return R.success("领取成功");
        }
        return R.error("领取失败，优惠券不可用或已领完");
    }

    /**
     * 优惠券模板业务校验（按类型校验金额/折扣率合法性）
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

