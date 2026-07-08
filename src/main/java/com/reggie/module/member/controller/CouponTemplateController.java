package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.service.CouponTemplateService;
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
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member/coupon-template")
@Tag(name = "优惠券模板")
public class CouponTemplateController {

    @Autowired
    private CouponTemplateService couponTemplateService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询优惠券模板列表，支持按名称搜索，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "优惠券名称（可选，模糊查询）")
    public R<Page<CouponTemplate>> page(int page, int pageSize, String name) {
        Page<CouponTemplate> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), CouponTemplate::getName, name);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(CouponTemplate::getTenantId, tenantId);
        }
        qw.orderByAsc(CouponTemplate::getId);
        couponTemplateService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增优惠券", description = "创建新的优惠券模板")
    public R<String> save(@RequestBody CouponTemplate couponTemplate) {
        log.info("新增优惠券模板: {}", couponTemplate.getName());
        couponTemplate.setCreatedTime(LocalDateTime.now());
        couponTemplate.setUpdatedTime(LocalDateTime.now());
        couponTemplateService.save(couponTemplate);
        return R.success("新增优惠券成功");
    }

    @PutMapping
    @Operation(summary = "修改优惠券", description = "更新优惠券模板信息")
    public R<String> update(@RequestBody CouponTemplate couponTemplate) {
        log.info("修改优惠券模板: {}", couponTemplate.getId());
        couponTemplate.setUpdatedTime(LocalDateTime.now());
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

    @PostMapping("/claim")
    @Operation(summary = "领取优惠券", description = "会员领取优惠券模板")
    public R<String> claim(@RequestBody Map<String, Object> params) {
        Long memberId = Long.valueOf(params.get("memberId").toString());
        Long templateId = Long.valueOf(params.get("templateId").toString());
        boolean ok = couponTemplateService.claimCoupon(memberId, templateId);
        if (ok) {
            return R.success("领取成功");
        }
        return R.error("领取失败，优惠券不可用或已领完");
    }
}

