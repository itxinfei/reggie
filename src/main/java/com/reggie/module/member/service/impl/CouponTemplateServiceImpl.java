package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.member.mapper.CouponTemplateMapper;
import com.reggie.module.member.model.CouponEffectVO;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.ExpiringByTemplateVO;
import com.reggie.module.member.model.ExpiringCouponVO;
import com.reggie.module.member.model.IssuedMemberVO;
import com.reggie.module.member.model.Member;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.enums.CouponStatus;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.CouponUserService;
import com.reggie.module.member.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 优惠券模板服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {

    /** 用户优惠券服务 */
    @Autowired
    private CouponUserService couponUserService;

    /** 会员服务，用于条件筛选会员 */
    @Autowired
    private MemberService memberService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimCoupon(Long memberId, Long templateId) {
        CouponTemplate template = getById(templateId);
        if (template == null || template.getStatus() != 1) {
            return false;
        }
        // 租户归属校验：仅允许领取当前租户的优惠券模板
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(template.getTenantId())) {
            throw new CustomException("无权领取其他租户的优惠券");
        }

        // SQL 原子扣减：remain_count = remain_count - 1，WHERE remain_count > 0 防止超发
        LambdaUpdateWrapper<CouponTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplate::getId, templateId);
        wrapper.gt(CouponTemplate::getRemainCount, 0);
        wrapper.setSql("remain_count = remain_count - 1");
        boolean deducted = update(wrapper);

        if (!deducted) {
            return false; // 库存不足，领取失败
        }

        // 修改点：移除 check-then-act 防重复校验（并发下存在 TOCTOU 漏洞），
        // 改为依赖 coupon_user 表的 uk_member_template 唯一索引保证幂等；
        // 插入冲突时捕获 DuplicateKeyException，回滚 remain_count 并返回 false
        CouponUser couponUser = new CouponUser();
        couponUser.setMemberId(memberId);
        couponUser.setTemplateId(templateId);
        couponUser.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        couponUser.setStatus(CouponStatus.UNUSED.getValue());
        if (template.getValidDays() != null) {
            couponUser.setExpireTime(LocalDateTime.now().plusDays(template.getValidDays()));
        }
        try {
            couponUserService.save(couponUser);
        } catch (DuplicateKeyException e) {
            // 已领取过（唯一索引冲突），回滚已扣减的库存
            LambdaUpdateWrapper<CouponTemplate> rollbackWrapper = new LambdaUpdateWrapper<>();
            rollbackWrapper.eq(CouponTemplate::getId, templateId);
            rollbackWrapper.setSql("remain_count = remain_count + 1");
            update(rollbackWrapper);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireCoupons() {
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        qw.lt(CouponUser::getExpireTime, LocalDateTime.now());
        List<CouponUser> expiredList = couponUserService.list(qw);
        for (CouponUser cu : expiredList) {
            cu.setStatus(CouponStatus.EXPIRED.getValue());
        }
        couponUserService.updateBatchById(expiredList);
    }

    @Override
    public Map<String, Object> getStats() {
        // 修改点：后端聚合替代前端 pageSize=1000 拉全量；仅查询所需三列，租户条件由拦截器自动注入
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        qw.select(CouponTemplate::getStatus, CouponTemplate::getTotalCount, CouponTemplate::getRemainCount);
        List<CouponTemplate> list = list(qw);

        long totalCoupons = list.size();
        long enabledCount = list.stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).count();
        long disabledCount = list.stream().filter(c -> c.getStatus() != null && c.getStatus() == 0).count();
        long exhaustedCount = list.stream()
                .filter(c -> c.getRemainCount() != null && c.getRemainCount() <= 0).count();
        long totalIssued = list.stream()
                .filter(c -> c.getTotalCount() != null)
                .mapToLong(CouponTemplate::getTotalCount)
                .sum();
        long totalClaimed = list.stream().mapToLong(c -> {
            int total = c.getTotalCount() != null ? c.getTotalCount() : 0;
            int remain = c.getRemainCount() != null ? c.getRemainCount() : 0;
            return Math.max(0, total - remain);
        }).sum();
        String usageRate = totalIssued > 0
                ? String.format("%.1f%%", totalClaimed * 100.0 / totalIssued)
                : "0%";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCoupons", totalCoupons);
        result.put("enabledCount", enabledCount);
        result.put("disabledCount", disabledCount);
        result.put("exhaustedCount", exhaustedCount);
        result.put("claimedCount", totalClaimed);
        result.put("usageRate", usageRate);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchIssue(Long templateId, List<Long> memberIds) {
        // 模板校验（含租户归属校验）
        CouponTemplate template = getById(templateId);
        if (template == null || template.getStatus() != 1) {
            Map<String, Object> failResult = new LinkedHashMap<>();
            failResult.put("successCount", 0);
            failResult.put("failCount", 0);
            failResult.put("alreadyIssuedCount", 0);
            failResult.put("total", 0);
            return failResult;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(template.getTenantId())) {
            throw new CustomException("无权操作其他租户的优惠券模板");
        }

        // 租户过滤：仅允许向当前租户会员发放（按会员ID精确查询，非全表）
        List<Long> uniqueMemberIds = new ArrayList<>();
        if (currentTenantId != null) {
            List<Member> members = memberService.lambdaQuery()
                    .eq(Member::getTenantId, currentTenantId)
                    .in(Member::getId, memberIds)
                    .list();
            for (Member m : members) {
                if (!uniqueMemberIds.contains(m.getId())) {
                    uniqueMemberIds.add(m.getId());
                }
            }
        } else {
            for (Long memberId : memberIds) {
                if (!uniqueMemberIds.contains(memberId)) {
                    uniqueMemberIds.add(memberId);
                }
            }
        }

        int successCount = 0;
        int failCount = 0;
        int alreadyIssuedCount = 0;

        for (Long memberId : uniqueMemberIds) {
            boolean ok = claimCoupon(memberId, templateId);
            if (ok) {
                successCount++;
            } else {
                LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
                qw.eq(CouponUser::getMemberId, memberId);
                qw.eq(CouponUser::getTemplateId, templateId);
                CouponUser existing = couponUserService.getOne(qw);
                if (existing != null) {
                    alreadyIssuedCount++;
                } else {
                    failCount++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("alreadyIssuedCount", alreadyIssuedCount);
        result.put("total", uniqueMemberIds.size());
        return result;
    }

    @Override
    public Map<String, Object> issueByCondition(Long templateId, Map<String, Object> condition) {
        if (condition == null || condition.isEmpty()) {
            throw new IllegalArgumentException("条件不能为空");
        }

        Long currentTenantId = BaseContext.getCurrentTenantId();

        // 按条件查询会员（含租户过滤）
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<>();
        qw.eq(currentTenantId != null, Member::getTenantId, currentTenantId);
        qw.eq(Member::getStatus, 1); // 仅向启用状态的会员发放

        Object levelId = condition.get("levelId");
        if (levelId != null) {
            qw.eq(Member::getLevelId, Long.parseLong(levelId.toString()));
        }

        Object minPoints = condition.get("minPoints");
        if (minPoints != null) {
            qw.ge(Member::getPoints, Long.parseLong(minPoints.toString()));
        }

        Object maxPoints = condition.get("maxPoints");
        if (maxPoints != null) {
            qw.le(Member::getPoints, Long.parseLong(maxPoints.toString()));
        }

        Object minConsumption = condition.get("minConsumption");
        if (minConsumption != null) {
            qw.ge(Member::getTotalConsumption,
                    new BigDecimal(minConsumption.toString()));
        }

        Object maxConsumption = condition.get("maxConsumption");
        if (maxConsumption != null) {
            qw.le(Member::getTotalConsumption,
                    new BigDecimal(maxConsumption.toString()));
        }

        Object newMemberDays = condition.get("newMemberDays");
        if (newMemberDays != null) {
            int days = Integer.parseInt(newMemberDays.toString());
            LocalDateTime threshold = LocalDateTime.now().minusDays(days);
            qw.ge(Member::getCreatedTime, threshold);
        }

        List<Member> members = memberService.list(qw);
        if (members == null || members.isEmpty()) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("successCount", 0);
            emptyResult.put("failCount", 0);
            emptyResult.put("alreadyIssuedCount", 0);
            emptyResult.put("total", 0);
            return emptyResult;
        }

        List<Long> memberIds = new ArrayList<>();
        for (Member m : members) {
            memberIds.add(m.getId());
        }

        return batchIssue(templateId, memberIds);
    }

    /**
     * 分页查询某模板的发放会员明细
     * <p>
     * 1. 分页查询 coupon_user（按 templateId），按 created_time 倒序；
     * 2. 批量查询对应会员信息，组装为 IssuedMemberVO 列表；
     * 3. 返回带分页元数据的 Page 对象。
     * </p>
     */
    @Override
    public Page<IssuedMemberVO> issuedMembers(Page<IssuedMemberVO> page, Long templateId) {
        // 分页查 coupon_user
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getTemplateId, templateId);
        qw.orderByDesc(CouponUser::getCreatedTime);
        Page<CouponUser> couponPage = couponUserService.page(new Page<>(page.getCurrent(), page.getSize()), qw);

        Page<IssuedMemberVO> voPage = new Page<>();
        voPage.setCurrent(couponPage.getCurrent());
        voPage.setSize(couponPage.getSize());
        voPage.setTotal(couponPage.getTotal());

        List<CouponUser> records = couponPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        // 收集会员ID，批量查询会员避免 N+1
        List<Long> memberIds = new ArrayList<>();
        for (CouponUser cu : records) {
            memberIds.add(cu.getMemberId());
        }
        List<Member> members = memberService.listByIds(memberIds);
        Map<Long, Member> memberMap = new LinkedHashMap<>();
        for (Member m : members) {
            memberMap.put(m.getId(), m);
        }

        // 组装 VO
        List<IssuedMemberVO> voList = new ArrayList<>();
        for (CouponUser cu : records) {
            IssuedMemberVO vo = new IssuedMemberVO();
            vo.setCouponUserId(cu.getId());
            vo.setCouponStatus(cu.getStatus());
            vo.setCreatedTime(cu.getCreatedTime());
            vo.setUsedTime(cu.getUsedTime());
            vo.setExpireTime(cu.getExpireTime());

            Member member = memberMap.get(cu.getMemberId());
            vo.setMemberId(cu.getMemberId());
            if (member != null) {
                vo.setMemberName(member.getName());
                vo.setMemberPhone(member.getPhone());
                vo.setLevelName(member.getLevelName());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 查询某模板的投放效果聚合指标
     * <p>
     * 一次性查询 coupon_user 相关字段，统计 unused/used/expired 分布，
     * 计算发放率（issued/total）、使用率（used/issued）、活跃率（used/total）。
     * </p>
     */
    @Override
    public CouponEffectVO effect(Long templateId) {
        CouponTemplate template = getById(templateId);
        if (template == null) {
            return null;
        }

        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getTemplateId, templateId);
        qw.select(CouponUser::getStatus);
        List<CouponUser> couponUsers = couponUserService.list(qw);

        int totalIssued = template.getTotalCount() != null ? template.getTotalCount() : 0;
        int remain = template.getRemainCount() != null ? template.getRemainCount() : 0;
        int issued = Math.max(0, totalIssued - remain);

        int usedCount = 0, unusedCount = 0, expiredCount = 0;
        if (couponUsers != null) {
            for (CouponUser cu : couponUsers) {
                if (CouponStatus.USED.getValue().equals(cu.getStatus())) {
                    usedCount++;
                } else if (CouponStatus.EXPIRED.getValue().equals(cu.getStatus())) {
                    expiredCount++;
                } else {
                    unusedCount++;
                }
            }
        }

        CouponEffectVO vo = new CouponEffectVO();
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getName());
        vo.setType(template.getType());
        vo.setTotalCount(totalIssued);
        vo.setRemainCount(remain);
        vo.setIssuedCount(issued);
        vo.setUsedCount(usedCount);
        vo.setUnusedCount(unusedCount);
        vo.setExpiredCount(expiredCount);
        vo.setIssueRate(formatRate(issued, totalIssued));
        vo.setUsageRate(formatRate(usedCount, issued));
        vo.setActiveRate(formatRate(usedCount, totalIssued));
        vo.setDiscountAmount(template.getDiscountAmount());
        vo.setConditionAmount(template.getConditionAmount());
        return vo;
    }

    /**
     * 查询即将到期优惠券明细（分页）
     * <p>
     * 1. 分页查询 coupon_user（status='unused' + expireTime 在未来 days 天内）；
     * 2. 批量查询对应会员与模板，组装为 ExpiringCouponVO 列表。
     * </p>
     */
    @Override
    public Page<ExpiringCouponVO> expiringCoupons(Page<ExpiringCouponVO> page, int days, Long templateId, String phone) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        qw.ge(CouponUser::getExpireTime, now);
        qw.le(CouponUser::getExpireTime, now.plusDays(days));
        if (templateId != null) {
            qw.eq(CouponUser::getTemplateId, templateId);
        }
        qw.orderByAsc(CouponUser::getExpireTime);
        Page<CouponUser> couponPage = couponUserService.page(new Page<>(page.getCurrent(), page.getSize()), qw);

        Page<ExpiringCouponVO> voPage = new Page<>();
        voPage.setCurrent(couponPage.getCurrent());
        voPage.setSize(couponPage.getSize());
        voPage.setTotal(couponPage.getTotal());

        List<CouponUser> records = couponPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<Long> memberIds = new ArrayList<>();
        List<Long> templateIds = new ArrayList<>();
        for (CouponUser cu : records) {
            memberIds.add(cu.getMemberId());
            templateIds.add(cu.getTemplateId());
        }
        List<Member> members = memberService.listByIds(memberIds);
        List<CouponTemplate> templates = listByIds(templateIds);
        Map<Long, Member> memberMap = new LinkedHashMap<>();
        for (Member m : members) { memberMap.put(m.getId(), m); }
        Map<Long, CouponTemplate> templateMap = new LinkedHashMap<>();
        for (CouponTemplate t : templates) { templateMap.put(t.getId(), t); }

        List<ExpiringCouponVO> voList = new ArrayList<>();
        for (CouponUser cu : records) {
            ExpiringCouponVO vo = new ExpiringCouponVO();
            vo.setCouponUserId(cu.getId());
            vo.setMemberId(cu.getMemberId());
            vo.setCouponStatus(cu.getStatus());
            vo.setCouponCode(cu.getCode());
            vo.setCreatedTime(cu.getCreatedTime());
            vo.setExpireTime(cu.getExpireTime());

            Member member = memberMap.get(cu.getMemberId());
            if (member != null) {
                vo.setMemberName(member.getName());
                vo.setMemberPhone(member.getPhone());
            }

            CouponTemplate template = templateMap.get(cu.getTemplateId());
            if (template != null) {
                vo.setTemplateId(template.getId());
                vo.setTemplateName(template.getName());
                vo.setCouponType(template.getType());
                vo.setDiscountAmount(template.getDiscountAmount());
                vo.setConditionAmount(template.getConditionAmount());
                vo.setDiscountRate(template.getDiscountRate());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 查询已过期优惠券明细（分页）
     */
    @Override
    public Page<ExpiringCouponVO> expiredCoupons(Page<ExpiringCouponVO> page, Long templateId, String phone) {
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getStatus, CouponStatus.EXPIRED.getValue());
        if (templateId != null) {
            qw.eq(CouponUser::getTemplateId, templateId);
        }
        qw.orderByDesc(CouponUser::getExpireTime);
        Page<CouponUser> couponPage = couponUserService.page(new Page<>(page.getCurrent(), page.getSize()), qw);

        Page<ExpiringCouponVO> voPage = new Page<>();
        voPage.setCurrent(couponPage.getCurrent());
        voPage.setSize(couponPage.getSize());
        voPage.setTotal(couponPage.getTotal());

        List<CouponUser> records = couponPage.getRecords();
        if (records == null || records.isEmpty()) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<Long> memberIds = new ArrayList<>();
        List<Long> templateIds = new ArrayList<>();
        for (CouponUser cu : records) {
            memberIds.add(cu.getMemberId());
            templateIds.add(cu.getTemplateId());
        }
        List<Member> members = memberService.listByIds(memberIds);
        List<CouponTemplate> templates = listByIds(templateIds);
        Map<Long, Member> memberMap = new LinkedHashMap<>();
        for (Member m : members) { memberMap.put(m.getId(), m); }
        Map<Long, CouponTemplate> templateMap = new LinkedHashMap<>();
        for (CouponTemplate t : templates) { templateMap.put(t.getId(), t); }

        List<ExpiringCouponVO> voList = new ArrayList<>();
        for (CouponUser cu : records) {
            ExpiringCouponVO vo = new ExpiringCouponVO();
            vo.setCouponUserId(cu.getId());
            vo.setMemberId(cu.getMemberId());
            vo.setCouponStatus(cu.getStatus());
            vo.setCouponCode(cu.getCode());
            vo.setCreatedTime(cu.getCreatedTime());
            vo.setExpireTime(cu.getExpireTime());

            Member member = memberMap.get(cu.getMemberId());
            if (member != null) {
                vo.setMemberName(member.getName());
                vo.setMemberPhone(member.getPhone());
            }

            CouponTemplate template = templateMap.get(cu.getTemplateId());
            if (template != null) {
                vo.setTemplateId(template.getId());
                vo.setTemplateName(template.getName());
                vo.setCouponType(template.getType());
                vo.setDiscountAmount(template.getDiscountAmount());
                vo.setConditionAmount(template.getConditionAmount());
                vo.setDiscountRate(template.getDiscountRate());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 优惠券到期预警统计（按模板聚合）
     * <p>
     * 查询两类券：即将到期（unused + expireTime 在 [now, now+days]）与已过期（expired）。
     * 按 templateId 聚合数量 + 优惠总额，按即将到期数量倒序。
     * </p>
     */
    @Override
    public List<ExpiringByTemplateVO> expiringStats(int days) {
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<CouponUser> expiringQw = new LambdaQueryWrapper<>();
        expiringQw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        expiringQw.ge(CouponUser::getExpireTime, now);
        expiringQw.le(CouponUser::getExpireTime, now.plusDays(days));
        expiringQw.select(CouponUser::getTemplateId);
        List<CouponUser> expiringList = couponUserService.list(expiringQw);

        LambdaQueryWrapper<CouponUser> expiredQw = new LambdaQueryWrapper<>();
        expiredQw.eq(CouponUser::getStatus, CouponStatus.EXPIRED.getValue());
        expiredQw.select(CouponUser::getTemplateId);
        List<CouponUser> expiredList = couponUserService.list(expiredQw);

        Map<Long, Integer> expiringCountMap = new LinkedHashMap<>();
        Map<Long, Integer> expiredCountMap = new LinkedHashMap<>();
        for (CouponUser cu : expiringList) {
            Long tid = cu.getTemplateId();
            expiringCountMap.put(tid, expiringCountMap.getOrDefault(tid, 0) + 1);
        }
        for (CouponUser cu : expiredList) {
            Long tid = cu.getTemplateId();
            expiredCountMap.put(tid, expiredCountMap.getOrDefault(tid, 0) + 1);
        }

        // 收集所有模板ID
        Map<Long, Boolean> allTemplateIds = new LinkedHashMap<>();
        for (Long tid : expiringCountMap.keySet()) { allTemplateIds.put(tid, Boolean.TRUE); }
        for (Long tid : expiredCountMap.keySet()) { allTemplateIds.put(tid, Boolean.TRUE); }

        // 批量查询模板信息 + 金额（用于计算优惠总额）
        List<Long> tidList = new ArrayList<>();
        for (Long tid : allTemplateIds.keySet()) { tidList.add(tid); }
        List<CouponTemplate> templates = listByIds(tidList);
        Map<Long, CouponTemplate> templateMap = new LinkedHashMap<>();
        for (CouponTemplate t : templates) { templateMap.put(t.getId(), t); }

        // 查询即将到期 + 已过期的券明细（含 templateId），计算优惠总额
        LambdaQueryWrapper<CouponUser> fullExpQw = new LambdaQueryWrapper<>();
        fullExpQw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        fullExpQw.ge(CouponUser::getExpireTime, now);
        fullExpQw.le(CouponUser::getExpireTime, now.plusDays(days));
        fullExpQw.select(CouponUser::getTemplateId);
        List<CouponUser> fullExpiringList = couponUserService.list(fullExpQw);

        // 已过期券（含 templateId）的优惠总额
        Map<Long, Integer> expiredCountFinalMap = new LinkedHashMap<>();
        for (CouponUser cu : expiredList) {
            Long tid = cu.getTemplateId();
            expiredCountFinalMap.put(tid, expiredCountFinalMap.getOrDefault(tid, 0) + 1);
        }

        // 计算各模板即将到期优惠总额：模板 discountAmount × expiringCount
        List<ExpiringByTemplateVO> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : expiringCountMap.entrySet()) {
            Long tid = entry.getKey();
            Integer cnt = entry.getValue();
            CouponTemplate tpl = templateMap.get(tid);
            ExpiringByTemplateVO vo = new ExpiringByTemplateVO();
            vo.setTemplateId(tid);
            if (tpl != null) {
                vo.setTemplateName(tpl.getName());
                vo.setCouponType(tpl.getType());
                BigDecimal amount = tpl.getDiscountAmount() != null
                        ? tpl.getDiscountAmount() : BigDecimal.ZERO;
                vo.setExpiringDiscountAmount(amount.multiply(new BigDecimal(cnt))
                        .setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setExpiringDiscountAmount(BigDecimal.ZERO);
            }
            vo.setExpiringCount(cnt);
            vo.setExpiredCount(expiredCountFinalMap.getOrDefault(tid, 0));

            // 已过期优惠总额
            CouponTemplate tplForExpired = templateMap.get(tid);
            if (tplForExpired != null && tplForExpired.getDiscountAmount() != null) {
                BigDecimal amt = tplForExpired.getDiscountAmount();
                int expCnt = expiredCountFinalMap.getOrDefault(tid, 0);
                vo.setExpiredDiscountAmount(amt.multiply(new BigDecimal(expCnt))
                        .setScale(2, RoundingMode.HALF_UP));
            } else {
                vo.setExpiredDiscountAmount(BigDecimal.ZERO);
            }
            result.add(vo);
        }

        // 排序：即将到期数量倒序
        List<ExpiringByTemplateVO> sorted = new ArrayList<>(result);
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                if (sorted.get(j).getExpiringCount() > sorted.get(i).getExpiringCount()) {
                    ExpiringByTemplateVO tmp = sorted.get(i);
                    sorted.set(i, sorted.get(j));
                    sorted.set(j, tmp);
                }
            }
        }
        return sorted;
    }

    /**
     * 批量延期优惠券
     * <p>
     * 对指定 coupon_user 列表（仅 status='unused'）向后延长 expireTime。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchExtend(List<Long> couponUserIds, int extendDays) {
        if (couponUserIds == null || couponUserIds.isEmpty() || extendDays <= 0) {
            Map<String, Object> emptyResult = new LinkedHashMap<>();
            emptyResult.put("successCount", 0);
            emptyResult.put("invalidCount", 0);
            emptyResult.put("total", 0);
            return emptyResult;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<CouponUser> coupons = currentTenantId != null
                ? couponUserService.lambdaQuery()
                        .eq(CouponUser::getTenantId, currentTenantId)
                        .in(CouponUser::getId, couponUserIds)
                        .list()
                : couponUserService.listByIds(couponUserIds);
        List<CouponUser> toUpdate = new ArrayList<>();
        int invalidCount = 0;
        for (CouponUser cu : coupons) {
            if (CouponStatus.UNUSED.getValue().equals(cu.getStatus()) && cu.getExpireTime() != null) {
                cu.setExpireTime(cu.getExpireTime().plusDays(extendDays));
                toUpdate.add(cu);
            } else {
                invalidCount++;
            }
        }

        if (!toUpdate.isEmpty()) {
            couponUserService.updateBatchById(toUpdate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("successCount", toUpdate.size());
        result.put("invalidCount", invalidCount);
        result.put("total", couponUserIds.size());
        return result;
    }

    /**
     * 新增优惠券模板（租户安全）
     * <p>tenantId 从 BaseContext 强制取得，前端无法通过 DTO 字段篡改租户归属。</p>
     */
    @Override
    public boolean addTenantCouponTemplate(String name, String type, BigDecimal conditionAmount,
                                           BigDecimal discountAmount, BigDecimal discountRate,
                                           Integer totalCount, Integer remainCount, Integer validDays, Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法创建优惠券模板");
        }
        CouponTemplate template = new CouponTemplate();
        template.setTenantId(tenantId);
        template.setName(name);
        template.setType(type);
        template.setConditionAmount(conditionAmount);
        template.setDiscountAmount(discountAmount);
        template.setDiscountRate(discountRate);
        template.setTotalCount(totalCount);
        template.setRemainCount(remainCount != null ? remainCount : totalCount);
        template.setValidDays(validDays);
        template.setStatus(status != null ? status : 1);
        template.setCreatedTime(java.time.LocalDateTime.now());
        template.setUpdateTime(java.time.LocalDateTime.now());
        return this.save(template);
    }

    /**
     * 更新优惠券模板（租户安全）
     * <p>先通过 id 查询确认该模板属于当前租户，再仅更新业务字段，绕过全实体覆盖漏洞。</p>
     */
    @Override
    public boolean updateTenantCouponTemplate(Long id, String name, String type, BigDecimal conditionAmount,
                                              BigDecimal discountAmount, BigDecimal discountRate,
                                              Integer totalCount, Integer remainCount, Integer validDays, Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法更新优惠券模板");
        }
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getId, id)
               .eq(CouponTemplate::getTenantId, tenantId);
        CouponTemplate existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("优惠券模板不存在或不属于当前租户（id=" + id + "）");
        }
        // 仅更新业务字段，不接收前端传入的 tenantId / id
        existing.setName(name);
        existing.setType(type);
        existing.setConditionAmount(conditionAmount);
        existing.setDiscountAmount(discountAmount);
        existing.setDiscountRate(discountRate);
        existing.setTotalCount(totalCount);
        existing.setRemainCount(remainCount);
        existing.setValidDays(validDays);
        existing.setStatus(status);
        existing.setUpdateTime(java.time.LocalDateTime.now());
        return this.updateById(existing);
    }

    /**
     * 删除优惠券模板（租户安全）
     * <p>先查询确认该模板属于当前租户，再删除，防止跨租户删除。</p>
     */
    @Override
    public boolean deleteTenantCouponTemplate(Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法删除优惠券模板");
        }
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplate::getId, id)
               .eq(CouponTemplate::getTenantId, tenantId);
        CouponTemplate existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("优惠券模板不存在或不属于当前租户（id=" + id + "）");
        }
        return this.removeById(id);
    }

    private String formatRate(int numerator, int denominator) {
        if (denominator <= 0) return "0%";
        return String.format("%.1f%%", numerator * 100.0 / denominator);
    }
}