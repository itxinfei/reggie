package com.reggie.module.member.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * C端会员自助控制器
 * 提供会员自助开通、在线发起充值（门店确认到账模式）、充值状态查询。
 * 不挂 @RequireEmployee：从登录态取 userId，禁止前端传 memberId/他人身份，配合归属校验防止越权。
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@RestController
@RequestMapping("/api/member/portal")
@Tag(name = "C端会员自助")
public class CustomerMemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private RechargeRecordService rechargeRecordService;

    @Autowired
    private UserService userService;

    /**
     * C端自助开通会员：手机号与姓名取当前登录用户真实信息，绑定 userId。
     * @return 新建会员
     */
    @PostMapping("/open")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "C端-自助开通会员", description = "登录用户一键开通会员，使用账号真实手机号与姓名")
    public R<Member> open() {
        Long userId = requireUser();
        User user = userService.getById(userId);
        if (user == null) {
            throw new CustomException("用户信息不存在，请重新登录");
        }
        Member member = memberService.registerForUser(userId, user.getPhone(), user.getName());
        log.info("[会员-自助开通] userId={}, phone={}", userId, maskPhone(user.getPhone()));
        return R.success(member);
    }

    /**
     * C端在线发起充值：创建待门店确认到账充值单。
     * @param body amount=充值金额，paymentMethod=意向渠道 WECHAT/ALIPAY（预留在线支付渠道）
     * @return 充值单摘要（充值单号/状态/金额/渠道）
     */
    @PostMapping("/recharge/create")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "C端-发起充值", description = "创建待门店确认到账充值单，门店收款确认后余额到账")
    public R<Map<String, Object>> createRecharge(@RequestBody Map<String, Object> body) {
        Long userId = requireUser();
        if (body == null) {
            throw new CustomException("请填写充值金额");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        Object pm = body.get("paymentMethod");
        String paymentMethod = pm == null ? null : String.valueOf(pm);

        RechargeRecord record = rechargeRecordService.createPendingRecharge(userId, amount, paymentMethod);
        log.info("[会员-发起充值] userId={}, rechargeNo={}, amount={}, channel={}",
                userId, record.getRechargeNo(), record.getAmount(), record.getPaymentMethod());
        return R.success(summarize(record));
    }

    /**
     * C端轮询充值状态（门店确认到账后前端据此刷新余额）。
     * @param rechargeNo 充值单号
     * @return 充值单状态摘要
     */
    @GetMapping("/recharge/status/{rechargeNo}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "C端-充值状态", description = "查询本人充值单状态，供门店确认到账后轮询")
    public R<Map<String, Object>> rechargeStatus(@PathVariable String rechargeNo) {
        Long userId = requireUser();
        RechargeRecord record = rechargeRecordService.getByRechargeNo(rechargeNo);
        if (record == null) {
            throw new CustomException("充值单不存在");
        }
        // 归属校验：userId 直接匹配；自助单必带 userId，防止同租户顾客越权查询他人充值单
        if (record.getUserId() == null || !userId.equals(record.getUserId())) {
            log.warn("[会员-充值状态] 越权访问被拦截: userId={}, rechargeNo={}", userId, rechargeNo);
            throw new CustomException("充值单不存在或无权查看");
        }
        return R.success(summarize(record));
    }

    /**
     * 取当前登录用户ID。
     * @return 用户ID
     */
    private Long requireUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new CustomException("请先登录");
        }
        return userId;
    }

    /**
     * 解析充值金额（兼容数字/字符串）。
     * @param value 原始值
     * @return 金额
     */
    private BigDecimal parseAmount(Object value) {
        if (value == null) {
            throw new CustomException("请填写充值金额");
        }
        try {
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new CustomException("充值金额格式不正确");
        }
    }

    /**
     * 组装充值单摘要（仅返回前端需要字段）。
     * @param record 充值记录
     * @return 摘要
     */
    private Map<String, Object> summarize(RechargeRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rechargeNo", record.getRechargeNo());
        data.put("status", record.getStatus());
        data.put("amount", record.getAmount());
        data.put("paymentMethod", record.getPaymentMethod());
        data.put("confirmTime", record.getConfirmTime());
        return data;
    }

    /**
     * 手机号脱敏（仅用于日志）。
     * @param phone 手机号
     * @return 脱敏结果
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
