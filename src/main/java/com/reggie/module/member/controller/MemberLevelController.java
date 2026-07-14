package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.MemberLevelService;
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

    /**
     * 分页查询会员等级列表
     * @param page 页码
     * @param pageSize 每页数量
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员等级列表，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<MemberLevel>> page(int page, int pageSize) {
        Page<MemberLevel> pageInfo = new Page<>(page, pageSize);
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
    @Operation(summary = "删除等级", description = "根据ID删除会员等级")
    @Parameter(name = "id", description = "等级ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        log.info("删除会员等级: {}", id);
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
}

