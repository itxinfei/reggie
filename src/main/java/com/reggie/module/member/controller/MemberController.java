package com.reggie.module.member.controller;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.LogMaskUtils;
import com.reggie.dto.DeductBalanceDTO;
import com.reggie.dto.RechargeDTO;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.MemberLevel;
import com.reggie.module.member.service.CouponUserService;
import com.reggie.module.member.service.MemberLevelService;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会员管理控制器
 * 提供会员的注册、查询、充值、余额扣减等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/member/member")
@Tag(name = "会员管理")
public class MemberController {

    private final MemberService memberService;
    private final RechargeRecordService rechargeRecordService;
    private final MemberLevelService memberLevelService;
    private final CouponUserService couponUserService;

    public MemberController(MemberService memberService, RechargeRecordService rechargeRecordService,
                            MemberLevelService memberLevelService, CouponUserService couponUserService) {
        this.memberService = memberService;
        this.rechargeRecordService = rechargeRecordService;
        this.memberLevelService = memberLevelService;
        this.couponUserService = couponUserService;
    }

    /**
     * 分页查询会员列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 会员姓名（可选，模糊查询）
     * @param phone 手机号（可选，模糊查询）
     * @return 分页结果
     */
    @GetMapping("/page")
    @RequireEmployee
    @Operation(summary = "分页查询", description = "分页查询会员列表，支持按姓名、手机号搜索，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "会员姓名（可选，模糊查询）")
    @Parameter(name = "phone", description = "手机号（可选，模糊查询）")
    public R<Page<Member>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, String name, String phone,
                                @RequestParam(required = false) Long levelId) {
        Page<Member> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Member::getName, name);
        qw.like(phone != null && !phone.isEmpty(), Member::getPhone, phone);
        qw.eq(levelId != null, Member::getLevelId, levelId);
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Member::getTenantId, tenantId);
        }
        qw.orderByAsc(Member::getId);
        memberService.page(pageInfo, qw);
        // 修改点：分页结果填充会员等级名称，供前端等级列展示
        memberService.fillLevelName(pageInfo.getRecords());
        return R.success(pageInfo);
    }

    /**
     * 会员统计看板（后端聚合，替代前端 pageSize=9999 拉全量后在浏览器计数）
     * 返回：会员总数、本月新增数、各等级会员数明细（levelCountMap）、无等级会员数（noLevelCount）
     * 前端据此结合等级积分门槛自行分级展示，避免传输全部会员数据。
     */
    @GetMapping("/stats")
    @RequireEmployee
    @Operation(summary = "会员统计", description = "统计会员总数、本月新增及各等级会员数量，后端聚合避免拉全量")
    public R<Map<String, Object>> stats() {
        // 租户隔离由 TenantLineInnerInterceptor 自动注入（memberService.count 与 countByLevel 原生 SQL 均生效）

        // 1. 会员总数（租户隔离由 TenantLineInnerInterceptor 自动注入）
        long totalMembers = memberService.count();

        // 2. 本月新增
        LocalDate now = LocalDate.now();
        LocalDateTime monthStart = LocalDateTime.of(now.withDayOfMonth(1), LocalTime.MIN);
        LocalDateTime monthEnd = LocalDateTime.of(now.withDayOfMonth(now.lengthOfMonth()), LocalTime.MAX);
        long newMembersThisMonth = memberService.count(new LambdaQueryWrapper<Member>()
                .ge(Member::getCreatedTime, monthStart)
                .le(Member::getCreatedTime, monthEnd));

        // 3. 按等级聚合（level_id 可能为 NULL，单独计入 noLevelCount）
        // 域4 改造：原始 SQL 执行下沉到 MemberService.countByLevel()，Controller 仅做结果展开
        Map<Long, Long> levelCountMap = new LinkedHashMap<>();
        long noLevelCount = 0;
        for (Map<String, Object> row : memberService.countByLevel()) {
            Object lid = row.get("levelId");
            long cnt = row.get("cnt") instanceof Number ? ((Number) row.get("cnt")).longValue() : 0L;
            if (lid == null) {
                noLevelCount = cnt;
            } else {
                levelCountMap.put(((Number) lid).longValue(), cnt);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMembers", totalMembers);
        result.put("newMembersThisMonth", newMembersThisMonth);
        result.put("levelCountMap", levelCountMap);
        result.put("noLevelCount", noLevelCount);
        return R.success(result);
    }

    /**
     * 新增会员
     * @param member 会员信息
     * @return 会员信息
     */
    @PostMapping
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增会员", description = "根据手机号注册新会员，自动生成会员卡号")
    public R<Member> save(@Valid @RequestBody Member member) {
        log.info("新增会员: {}", LogMaskUtils.maskPhone(member.getPhone()));
        Member result = memberService.registerByPhone(member.getPhone(), member.getName());
        return R.success(result);
    }

    /**
     * 修改会员
     * @param member 会员信息
     * @return 操作结果
     */
    @PutMapping
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改会员", description = "更新会员基本信息")
    public R<String> update(@Valid @RequestBody Member member) {
        if (member.getId() == null) {
            return R.error("会员ID不能为空");
        }
        // 修改点：先加载已存在记录并校验租户归属，防止跨租户越权改写
        Member existing = memberService.getById(member.getId());
        if (existing == null) {
            return R.error("会员不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            return R.error("无权操作其他租户的会员");
        }

        // 修改点：白名单字段更新，禁止越权改写 balance/points/levelId/tenantId 等资金与权限字段
        LambdaUpdateWrapper<Member> uw = new LambdaUpdateWrapper<>();
        uw.eq(Member::getId, member.getId());
        if (member.getName() != null) {
            uw.set(Member::getName, member.getName());
        }
        if (member.getPhone() != null) {
            uw.set(Member::getPhone, member.getPhone());
        }
        if (member.getStatus() != null) {
            uw.set(Member::getStatus, member.getStatus());
        }
        uw.set(Member::getUpdateTime, LocalDateTime.now());

        log.info("修改会员: {}", member.getId());
        memberService.update(uw);
        return R.success("修改会员成功");
    }

    /**
     * 根据ID查询会员
     * @param id 会员ID
     * @return 会员详情
     */
    @GetMapping("/{id}")
    @RequireEmployee
    @Operation(summary = "查询会员", description = "根据ID查询会员详情")
    @Parameter(name = "id", description = "会员ID", required = true)
    public R<Member> getById(@PathVariable Long id) {
        Member member = memberService.getById(id);
        if (member != null) {
            // 修改点：填充会员等级名称，供详情弹窗展示
            memberService.fillLevelName(Collections.singletonList(member));
            return R.success(member);
        }
        return R.error("没有查询到对应会员");
    }

    /**
     * 收银台：按手机号识别会员
     * <p>门店收银时录入会员手机号，返回会员信息（含积分、余额），用于积分发放与储值抵扣。</p>
     */
    @GetMapping("/by-phone")
    @RequireEmployee
    @Operation(summary = "按手机号查询会员", description = "收银台会员识别：传入手机号返回会员信息")
    @Parameter(name = "phone", description = "会员手机号", required = true)
    public R<Member> getByPhone(@RequestParam String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return R.error("手机号不能为空");
        }
        Member member = memberService.lambdaQuery().eq(Member::getPhone, phone.trim()).one();
        if (member != null) {
            memberService.fillLevelName(Collections.singletonList(member));
            return R.success(member);
        }
        return R.error("未找到该手机号的会员");
    }

    /**
     * 会员充值
     * <p>租户安全：先校验会员归属当前租户，防止越权为其他租户会员充值。</p>
     * @param dto 充值请求
     * @return 操作结果
     */
    @PostMapping("/recharge")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "会员充值", description = "为会员账户充值，支持赠送金额")
    public R<String> recharge(@Validated @RequestBody RechargeDTO dto) {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Member member = memberService.getById(dto.getMemberId());
        if (member == null) {
            return R.error("会员不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            return R.error("无权操作其他租户的会员");
        }
        rechargeRecordService.recharge(dto.getMemberId(), dto.getAmount(), dto.getGiftAmount(), dto.getPaymentMethod());
        log.info("会员充值: memberId={}, amount={}", dto.getMemberId(), dto.getAmount());
        return R.success("充值成功");
    }

    /**
     * 扣减会员余额
     * <p>租户安全：先校验会员归属当前租户，防止越权扣减其他租户会员余额。</p>
     * @param dto 余额扣减请求
     * @return 操作结果
     */
    @PostMapping("/deduct-balance")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "扣减余额", description = "扣减会员账户余额（用于订单抵扣等）")
    public R<String> deductBalance(@Validated @RequestBody DeductBalanceDTO dto) {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Member member = memberService.getById(dto.getMemberId());
        if (member == null) {
            return R.error("会员不存在");
        }
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            return R.error("无权操作其他租户的会员");
        }
        boolean ok = memberService.deductBalance(dto.getMemberId(), dto.getAmount());
        if (ok) {
            return R.success("扣减成功");
        }
        return R.error("余额不足或会员不存在");
    }

    /**
     * C端：获取当前登录用户的会员信息
     * 根据当前用户ID查询对应的会员信息，含等级详情、优惠券数量
     */
    @GetMapping("/my-info")
    @Operation(summary = "C端-会员信息", description = "获取当前登录用户的会员信息：等级、积分、余额、可用优惠券数量、折扣率")
    public R<Map<String, Object>> myInfo() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return R.error("用户未登录");
        }
        Long tenantId = BaseContext.getCurrentTenantId();

        // 查询会员信息
        LambdaQueryWrapper<Member> memberQw = new LambdaQueryWrapper<>();
        memberQw.eq(Member::getUserId, userId);
        if (tenantId != null) memberQw.eq(Member::getTenantId, tenantId);
        Member member = memberService.getOne(memberQw);

        Map<String, Object> result = new LinkedHashMap<>();
        if (member == null) {
            // 用户尚未注册为会员
            result.put("isMember", false);
            result.put("member", null);
            result.put("level", null);
            result.put("couponCount", 0);
            return R.success(result);
        }

        result.put("isMember", true);
        result.put("member", member);

        // 查询等级信息
        if (member.getLevelId() != null) {
            MemberLevel level = memberLevelService.getById(member.getLevelId());
            result.put("level", level);
        } else {
            result.put("level", null);
        }

        // 查询可用优惠券数量
        LambdaQueryWrapper<CouponUser> couponQw = new LambdaQueryWrapper<>();
        couponQw.eq(CouponUser::getMemberId, member.getId());
        if (tenantId != null) couponQw.eq(CouponUser::getTenantId, tenantId);
        couponQw.eq(CouponUser::getStatus, "unused");
        int couponCount = (int) couponUserService.count(couponQw);
        result.put("couponCount", couponCount);

        return R.success(result);
    }
}

