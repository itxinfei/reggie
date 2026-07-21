package com.reggie.module.member.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.MemberLevelService;
import com.reggie.module.member.service.MemberService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会员等级管理控制器
 * 提供会员等级的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/member/level")
@Tag(name = "会员等级管理")
public class MemberLevelController {

    @Autowired
    private MemberLevelService memberLevelService;

    @Autowired
    private MemberService memberService;

    /**
     * 分页查询会员等级列表
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员等级列表，自动过滤当前租户数据")
    public R<Page<MemberLevel>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<MemberLevel> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<MemberLevel> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(MemberLevel::getTenantId, tenantId);
        }
        qw.orderByAsc(MemberLevel::getMinPoints);
        memberLevelService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 所有会员等级列表
     */
    @GetMapping("/list")
    @Operation(summary = "等级列表", description = "获取所有会员等级列表")
    public R<List<MemberLevel>> list() {
        LambdaQueryWrapper<MemberLevel> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(MemberLevel::getTenantId, tenantId);
        }
        qw.orderByAsc(MemberLevel::getMinPoints);
        return R.success(memberLevelService.list(qw));
    }

    /**
     * 新增会员等级
     * @param memberLevel 会员等级信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增等级", description = "创建新的会员等级")
    public R<String> save(@RequestBody MemberLevel memberLevel) {
        log.info("新增会员等级: {}", memberLevel.getName());
        memberLevel.setCreatedTime(LocalDateTime.now());
        memberLevelService.save(memberLevel);
        return R.success("新增会员等级成功");
    }

    /**
     * 修改会员等级
     * @param memberLevel 会员等级信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改等级", description = "更新会员等级信息")
    public R<String> update(@RequestBody MemberLevel memberLevel) {
        log.info("修改会员等级: {}", memberLevel.getId());
        memberLevelService.updateById(memberLevel);
        return R.success("修改会员等级成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除等级", description = "根据ID删除会员等级，删除前校验是否仍有会员引用该等级")
    @Parameter(name = "id", description = "等级ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除会员等级: {}", id);
        // 修改点：删除前校验是否存在会员引用该等级，避免产生孤儿数据（会员.levelId 悬空）
        long refCount = memberService.count(new LambdaQueryWrapper<Member>().eq(Member::getLevelId, id));
        if (refCount > 0) {
            throw new CustomException("该等级下仍存在 " + refCount + " 名会员，无法删除");
        }
        memberLevelService.removeById(id);
        return R.success("删除会员等级成功");
    }

    /**
     * 根据ID查询会员等级
     * @param id 等级ID
     * @return 等级详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询等级", description = "根据ID查询会员等级详情")
    @Parameter(name = "id", description = "等级ID", required = true)
    public R<MemberLevel> getById(@PathVariable Long id) {
        MemberLevel level = memberLevelService.getById(id);
        if (level != null) {
            return R.success(level);
        }
        return R.error("没有查询到对应会员等级");
    }

    /**
     * 会员等级统计（等级管理页用）
     * <p>使用聚合查询替代前端仅基于当前页 records 计算统计（避免"仅当前页"口径偏差）</p>
     *
     * @return 等级总数、最高等级名、入门最低积分、平均折扣率
     */
    @GetMapping("/stats")
    @Operation(summary = "会员等级统计", description = "聚合统计等级总数、最高等级、入门门槛、平均折扣率")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<MemberLevel> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(MemberLevel::getTenantId, tenantId);
        }
        long totalLevels = memberLevelService.count(qw);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLevels", totalLevels);
        if (totalLevels == 0) {
            result.put("highestLevel", "-");
            result.put("lowestPoints", 0);
            result.put("avgDiscount", "-");
            return R.success(result);
        }

        List<MemberLevel> levels = memberLevelService.list(qw);
        MemberLevel highest = levels.stream()
                .max(java.util.Comparator.comparing(l -> l.getMinPoints() == null ? 0 : l.getMinPoints()))
                .orElse(null);
        result.put("highestLevel", highest != null && highest.getName() != null ? highest.getName() : "-");

        Long lowest = levels.stream()
                .map(MemberLevel::getMinPoints)
                .filter(p -> p != null && p > 0)
                .min(Long::compare)
                .orElse(0L);
        result.put("lowestPoints", lowest);

        java.util.List<MemberLevel> valid = levels.stream()
                .filter(l -> l.getDiscount() != null && l.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0)
                .collect(java.util.stream.Collectors.toList());
        if (valid.isEmpty()) {
            result.put("avgDiscount", "-");
        } else {
            double avg = valid.stream()
                    .mapToDouble(l -> l.getDiscount() != null ? l.getDiscount().doubleValue() : 0d)
                    .average()
                    .orElse(0d);
            result.put("avgDiscount", Math.round(avg * 100) + "%");
        }
        return R.success(result);
    }
}

