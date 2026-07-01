package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/member/member")
@Tag(name = "会员管理")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<Member>> page(int page, int pageSize, String name, String phone) {
        Page<Member> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Member::getName, name);
        qw.like(phone != null && !phone.isEmpty(), Member::getPhone, phone);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Member::getTenantId, tenantId);
        }
        qw.orderByAsc(Member::getId);
        memberService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增会员")
    public R<Member> save(@RequestBody Member member) {
        log.info("新增会员: {}", member.getPhone());
        Member result = memberService.registerByPhone(member.getPhone(), member.getName());
        return R.success(result);
    }

    @PutMapping
    @Operation(summary = "修改会员")
    public R<String> update(@RequestBody Member member) {
        log.info("修改会员: {}", member.getId());
        member.setUpdatedTime(LocalDateTime.now());
        memberService.updateById(member);
        return R.success("修改会员成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询会员")
    public R<Member> getById(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member != null) {
            return R.success(member);
        }
        return R.error("没有查询到对应会员");
    }

    @Autowired
    private com.reggie.module.member.service.RechargeRecordService rechargeRecordService;

    @PostMapping("/recharge")
    @Operation(summary = "会员充值")
    public R<String> recharge(@RequestBody Map<String, Object> params) {
        Long memberId = Long.valueOf(params.get("memberId").toString());
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        BigDecimal giftAmount = params.get("giftAmount") != null ? new BigDecimal(params.get("giftAmount").toString()) : BigDecimal.ZERO;
        String paymentMethod = (String) params.get("paymentMethod");
        rechargeRecordService.recharge(memberId, amount, giftAmount, paymentMethod);
        log.info("会员充值: memberId={}, amount={}", memberId, amount);
        return R.success("充值成功");
    }

    @PostMapping("/deduct-balance")
    @Operation(summary = "扣减余额")
    public R<String> deductBalance(@RequestBody Map<String, Object> params) {
        Long memberId = Long.valueOf(params.get("memberId").toString());
        BigDecimal amount = new BigDecimal(params.get("amount").toString());
        boolean ok = memberService.deductBalance(memberId, amount);
        if (ok) {
            return R.success("扣减成功");
        }
        return R.error("余额不足或会员不存在");
    }
}
