package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.mapper.RechargeRecordMapper;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 充值记录服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class RechargeRecordServiceImpl extends ServiceImpl<RechargeRecordMapper, RechargeRecord> implements
        RechargeRecordService {

    /** C端在线发起充值金额上限（元），配合限流防止异常大额挂单 */
    private static final BigDecimal MAX_PORTAL_AMOUNT = new BigDecimal("5000");

    /** 会员服务 */
    @Autowired
    private MemberService memberService;

    /** 会员Mapper（用于原子加余额） */
    @Autowired
    private MemberMapper memberMapper;

    /** 充值记录Mapper（用于门店确认 CAS） */
    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    /**
     * 处理 recharge。
     * @param memberId 参数 memberId
     * @param amount 参数 amount
     * @param giftAmount 参数 giftAmount
     * @param paymentMethod 参数 paymentMethod
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long memberId, BigDecimal amount, BigDecimal giftAmount, String paymentMethod) {
        // 修改点：原子加余额（balance = balance + amount + IFNULL(giftAmount, 0)），
        // 消除 read-modify-write 并发丢失更新；先校验会员存在性
        Member member = memberService.getById(memberId);
        if (member == null) {
            throw new CustomException("会员不存在");
        }
        // 租户归属校验：防止跨租户盗充
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            throw new CustomException("无权操作其他租户的会员储值");
        }

        int rows = memberMapper.addBalance(memberId, amount, giftAmount);
        if (rows == 0) {
            throw new CustomException("会员不存在");
        }

        RechargeRecord record = new RechargeRecord();
        record.setMemberId(memberId);
        record.setAmount(amount);
        record.setGiftAmount(giftAmount);
        record.setPaymentMethod(paymentMethod);
        // 员工代办即时入账：直接置 SUCCESS 并生成单号，与 C 端挂单共用同一状态口径
        record.setStatus(RechargeRecord.STATUS_SUCCESS);
        record.setRechargeNo(generateRechargeNo());
        record.setConfirmTime(java.time.LocalDateTime.now());
        save(record);
    }

    /**
     * C端创建待确认充值单。
     * @param userId 用户ID
     * @param amount 金额
     * @param paymentMethod 意向渠道
     * @return 待确认充值记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeRecord createPendingRecharge(Long userId, BigDecimal amount, String paymentMethod) {
        if (userId == null) {
            throw new CustomException("请先登录");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("充值金额必须大于0");
        }
        if (amount.compareTo(MAX_PORTAL_AMOUNT) > 0) {
            throw new CustomException("单笔充值金额不能超过" + MAX_PORTAL_AMOUNT + "元");
        }
        String channel = normalizeChannel(paymentMethod);

        Member member = memberService.getByUserId(userId);
        if (member == null) {
            throw new CustomException("请先开通会员后再充值");
        }

        // 已有待确认单则复用，避免同一用户重复挂单
        RechargeRecord existing = lambdaQuery()
                .eq(RechargeRecord::getMemberId, member.getId())
                .eq(RechargeRecord::getStatus, RechargeRecord.STATUS_PENDING)
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            return existing;
        }

        RechargeRecord record = new RechargeRecord();
        record.setMemberId(member.getId());
        record.setUserId(userId);
        record.setAmount(amount);
        record.setPaymentMethod(channel);
        record.setStatus(RechargeRecord.STATUS_PENDING);
        record.setRechargeNo(generateRechargeNo());
        save(record);
        return record;
    }

    /**
     * 门店确认到账。
     * @param rechargeNo 充值单号
     * @param employeeId 员工ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRecharge(String rechargeNo, Long employeeId) {
        if (rechargeNo == null || rechargeNo.trim().isEmpty()) {
            throw new CustomException("充值单号不能为空");
        }
        RechargeRecord record = getByRechargeNo(rechargeNo.trim());
        if (record == null) {
            throw new CustomException("充值单不存在");
        }
        // 先 CAS 状态机（PENDING→SUCCESS），失败即重复/已取消，直接拦截，保证每单只入账一次
        int casRows = rechargeRecordMapper.casConfirm(record.getId(), employeeId);
        if (casRows == 0) {
            throw new CustomException("充值单已确认或已取消，请勿重复操作");
        }
        // CAS 成功后原子加余额；若加钱失败抛异常，整事务回滚（CAS 一并撤销），状态与余额保持一致
        int rows = memberMapper.addBalance(record.getMemberId(), record.getAmount(), record.getGiftAmount());
        if (rows == 0) {
            throw new CustomException("会员不存在，入账失败");
        }
    }

    /**
     * 按单号查询。
     * @param rechargeNo 充值单号
     * @return 充值记录
     */
    @Override
    public RechargeRecord getByRechargeNo(String rechargeNo) {
        return lambdaQuery().eq(RechargeRecord::getRechargeNo, rechargeNo).one();
    }

    /**
     * 渠道白名单：仅接受微信/支付宝意向（预留在线支付渠道），非法值兜底为微信。
     * @param paymentMethod 原始渠道
     * @return 规范渠道
     */
    private String normalizeChannel(String paymentMethod) {
        if ("ALIPAY".equals(paymentMethod)) {
            return "ALIPAY";
        }
        return "WECHAT";
    }

    /**
     * 生成充值单号：RC + yyyyMMddHHmmss + UUID8，保证本地唯一（时间戳+随机串，规避同秒并发冲突）。
     * @return 充值单号
     */
    private String generateRechargeNo() {
        String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RC" + ts + rand;
    }
}
