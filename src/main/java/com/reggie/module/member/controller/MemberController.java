package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.DeductBalanceDTO;
import com.reggie.dto.RechargeDTO;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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

@Slf4j
@RestController
@RequestMapping("/api/member/member")
@Tag(name = "会员管理")
public class MemberController {

    private final MemberService memberService;
    private final RechargeRecordService rechargeRecordService;

    public MemberController(MemberService memberService, RechargeRecordService rechargeRecordService) {
        this.memberService = memberService;
        this.rechargeRecordService = rechargeRecordService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员列表，支持按姓名、手机号搜索，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "会员姓名（可选，模糊查询）")
    @Parameter(name = "phone", description = "手机号（可选，模糊查询）")
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
    @Operation(summary = "新增会员", description = "根据手机号注册新会员，自动生成会员卡号")
    public R<Member> save(@RequestBody Member member) {
        log.info("新增会员: {}", member.getPhone());
        Member result = memberService.registerByPhone(member.getPhone(), member.getName());
        return R.success(result);
    }

    @PutMapping
    @Operation(summary = "修改会员", description = "更新会员基本信息")
    public R<String> update(@RequestBody Member member) {
        log.info("修改会员: {}", member.getId());
        member.setUpdatedTime(LocalDateTime.now());
        memberService.updateById(member);
        return R.success("修改会员成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询会员", description = "根据ID查询会员详情")
    @Parameter(name = "id", description = "会员ID", required = true)
    public R<Member> getById(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member != null) {
            return R.success(member);
        }
        return R.error("没有查询到对应会员");
    }

    @PostMapping("/recharge")
    @Operation(summary = "会员充值", description = "为会员账户充值，支持赠送金额")
    public R<String> recharge(@Validated @RequestBody RechargeDTO dto) {
        rechargeRecordService.recharge(dto.getMemberId(), dto.getAmount(), dto.getGiftAmount(), dto.getPaymentMethod());
        log.info("会员充值: memberId={}, amount={}", dto.getMemberId(), dto.getAmount());
        return R.success("充值成功");
    }

    @PostMapping("/deduct-balance")
    @Operation(summary = "扣减余额", description = "扣减会员账户余额（用于订单抵扣等）")
    public R<String> deductBalance(@Validated @RequestBody DeductBalanceDTO dto) {
        boolean ok = memberService.deductBalance(dto.getMemberId(), dto.getAmount());
        if (ok) {
            return R.success("扣减成功");
        }
        return R.error("余额不足或会员不存在");
    }
}

