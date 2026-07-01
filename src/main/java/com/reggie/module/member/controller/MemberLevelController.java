package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.MemberLevelService;
import io.swagger.v3.oas.annotations.Operation;
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

@Slf4j
@RestController
@RequestMapping("/api/member/level")
@Tag(name = "会员等级管理")
public class MemberLevelController {

    @Autowired
    private MemberLevelService memberLevelService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
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

    @PostMapping
    @Operation(summary = "新增等级")
    public R<String> save(@RequestBody MemberLevel memberLevel) {
        log.info("新增会员等级: {}", memberLevel.getName());
        memberLevel.setCreatedTime(LocalDateTime.now());
        memberLevelService.save(memberLevel);
        return R.success("新增会员等级成功");
    }

    @PutMapping
    @Operation(summary = "修改等级")
    public R<String> update(@RequestBody MemberLevel memberLevel) {
        log.info("修改会员等级: {}", memberLevel.getId());
        memberLevelService.updateById(memberLevel);
        return R.success("修改会员等级成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除等级")
    public R<String> delete(@PathVariable Long id) {
        log.info("删除会员等级: {}", id);
        memberLevelService.removeById(id);
        return R.success("删除会员等级成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询等级")
    public R<MemberLevel> getById(@PathVariable Long id) {
        MemberLevel level = memberLevelService.getById(id);
        if (level != null) {
            return R.success(level);
        }
        return R.error("没有查询到对应会员等级");
    }
}
