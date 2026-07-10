package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.BatchRemoveMemberTagDTO;
import com.reggie.enums.MemberBizTag;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberTag;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.MemberTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 会员标签控制器
 *
 * @author reggie
 * @since 2026-07-10
 */
@Slf4j
@RestController
@RequestMapping("/api/member/member-tag")
@Tag(name = "会员标签管理")
public class MemberTagController {

    @Autowired
    private MemberTagService memberTagService;

    @Autowired
    private MemberService memberService;

    @PostMapping("/member/{memberId}/tags")
    @Operation(summary = "为会员添加标签", description = "为指定会员添加业务标签")
    public R<String> addTag(
            @Parameter(name = "memberId", description = "会员ID", required = true)
            @PathVariable Long memberId,
            @Parameter(name = "tagName", description = "标签名称", required = true)
            @RequestParam String tagName,
            @Parameter(name = "bizTag", description = "业务标签", required = true)
            @RequestParam String bizTag,
            @Parameter(name = "tagColor", description = "标签颜色", required = true)
            @RequestParam String tagColor) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        boolean success = memberTagService.addTag(tenantId, memberId, tagName, bizTag, tagColor);
        if (success) {
            return R.success("添加标签成功");
        }
        return R.error("添加标签失败");
    }

    @DeleteMapping("/member/{memberId}/tags/{tagId}")
    @Operation(summary = "删除会员标签", description = "删除指定会员的指定标签")
    public R<String> removeTag(
            @Parameter(name = "memberId", description = "会员ID", required = true)
            @PathVariable Long memberId,
            @Parameter(name = "tagId", description = "标签ID", required = true)
            @PathVariable Long tagId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        LambdaQueryWrapper<MemberTag> qw = new LambdaQueryWrapper<>();
        qw.eq(MemberTag::getTenantId, tenantId);
        qw.eq(MemberTag::getMemberId, memberId);
        qw.eq(MemberTag::getId, tagId);
        boolean success = memberTagService.remove(qw);
        if (success) {
            return R.success("删除标签成功");
        }
        return R.error("删除标签失败");
    }

    @DeleteMapping("/member/{memberId}/tags/batch")
    @Operation(summary = "批量删除标签", description = "批量删除指定会员的标签")
    public R<String> batchRemoveTags(
            @Parameter(name = "memberId", description = "会员ID", required = true)
            @PathVariable Long memberId,
            @Validated @RequestBody BatchRemoveMemberTagDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        boolean success = memberTagService.batchRemoveTags(tenantId, memberId, dto.getTagIds());
        if (success) {
            return R.success("批量删除标签成功");
        }
        return R.error("批量删除标签失败");
    }

    @GetMapping("/member/{memberId}/tags")
    @Operation(summary = "查询会员标签", description = "查询指定会员的所有标签")
    public R<List<MemberTag>> listByMemberId(
            @Parameter(name = "memberId", description = "会员ID", required = true)
            @PathVariable Long memberId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        List<MemberTag> tags = memberTagService.listByMemberId(tenantId, memberId);
        return R.success(tags);
    }

    @GetMapping("/tags/types")
    @Operation(summary = "获取标签类型列表", description = "获取所有标签类型（1手动添加 2自动生成）")
    public R<List<Map<String, Object>>> getTagTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        types.add(createTypeMap(1, "手动添加"));
        types.add(createTypeMap(2, "自动生成"));
        return R.success(types);
    }

    @GetMapping("/tags/biz-tags/count")
    @Operation(summary = "各业务标签数量统计", description = "统计各业务标签的会员数量")
    public R<Map<String, Long>> countByBizTag() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        Map<String, Long> countMap = memberTagService.countByBizTag(tenantId);
        return R.success(countMap);
    }

    @GetMapping("/tags/biz-tag/{bizTag}/members")
    @Operation(summary = "按业务标签查询会员", description = "查询具有指定业务标签的会员列表")
    public R<Page<Member>> getMembersByBizTag(
            @Parameter(name = "bizTag", description = "业务标签", required = true)
            @PathVariable String bizTag,
            @Parameter(name = "page", description = "页码", required = true)
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(name = "pageSize", description = "每页数量", required = true)
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        LambdaQueryWrapper<MemberTag> tagQw = new LambdaQueryWrapper<>();
        tagQw.eq(MemberTag::getTenantId, tenantId);
        tagQw.eq(MemberTag::getBizTag, bizTag);
        List<MemberTag> tags = memberTagService.list(tagQw);

        if (tags.isEmpty()) {
            return R.success(new Page<>());
        }

        Set<Long> memberIds = tags.stream()
                .map(MemberTag::getMemberId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
        memberQw.in(Member::getId, memberIds);
        memberQw.orderByDesc(Member::getCreatedTime);

        Page<Member> pageInfo = new Page<>(page, pageSize);
        memberService.page(pageInfo, memberQw);
        return R.success(pageInfo);
    }

    @PostMapping("/auto-generate")
    @Operation(summary = "自动生成标签", description = "根据会员消费行为自动生成标签（定时任务调用）")
    public R<String> autoGenerateTags() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }
        int count = memberTagService.autoGenerateTags(tenantId);
        return R.success("自动生成标签完成，共生成 " + count + " 个标签");
    }

    private Map<String, Object> createTypeMap(Integer value, String desc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", value);
        map.put("desc", desc);
        return map;
    }
}
