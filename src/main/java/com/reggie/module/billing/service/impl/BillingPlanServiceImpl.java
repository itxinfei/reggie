package com.reggie.module.billing.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.billing.mapper.BillingPlanMapper;
import com.reggie.module.billing.model.BillingPlan;
import com.reggie.module.billing.service.BillingPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * SaaS套餐方案服务实现
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@Service
public class BillingPlanServiceImpl extends ServiceImpl<BillingPlanMapper, BillingPlan> implements BillingPlanService {

    @Override
    public Page<BillingPlan> pageQuery(int page, int pageSize, String keyword, Integer status) {
        Page<BillingPlan> p = PageUtils.of(page, pageSize);
        String kw = keyword == null ? null : keyword.trim();
        boolean hasKw = kw != null && !kw.isEmpty();
        return lambdaQuery()
                .and(hasKw, w -> w.like(BillingPlan::getPlanName, kw).or().like(BillingPlan::getPlanCode, kw))
                .eq(status != null, BillingPlan::getStatus, status)
                .orderByAsc(BillingPlan::getSortOrder)
                .orderByDesc(BillingPlan::getId)
                .page(p);
    }

    @Override
    public List<BillingPlan> listOnShelf() {
        return lambdaQuery()
                .eq(BillingPlan::getStatus, BillingPlan.STATUS_ON)
                .orderByAsc(BillingPlan::getSortOrder)
                .orderByAsc(BillingPlan::getId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BillingPlan createPlan(BillingPlan plan) {
        validatePlan(plan);
        if (plan.getPlanCode() == null || plan.getPlanCode().trim().isEmpty()) {
            throw new CustomException("套餐编码不能为空");
        }
        Long codeCount = lambdaQuery().eq(BillingPlan::getPlanCode, plan.getPlanCode().trim()).count();
        if (codeCount != null && codeCount > 0) {
            throw new CustomException("套餐编码已存在：" + plan.getPlanCode());
        }
        plan.setPlanCode(plan.getPlanCode().trim());
        if (plan.getStatus() == null) {
            plan.setStatus(BillingPlan.STATUS_ON);
        }
        if (plan.getSortOrder() == null) {
            plan.setSortOrder(0);
        }
        save(plan);
        log.info("[SaaS计费] 新增套餐 id={}, code={}", plan.getId(), plan.getPlanCode());
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(BillingPlan plan) {
        if (plan.getId() == null) {
            throw new CustomException("套餐ID不能为空");
        }
        BillingPlan exist = getById(plan.getId());
        if (exist == null) {
            throw new CustomException("套餐不存在");
        }
        validatePlan(plan);
        if (plan.getPlanCode() != null && !plan.getPlanCode().trim().equals(exist.getPlanCode())) {
            Long codeCount = lambdaQuery()
                    .eq(BillingPlan::getPlanCode, plan.getPlanCode().trim())
                    .ne(BillingPlan::getId, plan.getId())
                    .count();
            if (codeCount != null && codeCount > 0) {
                throw new CustomException("套餐编码已存在：" + plan.getPlanCode());
            }
            plan.setPlanCode(plan.getPlanCode().trim());
        }
        updateById(plan);
        log.info("[SaaS计费] 更新套餐 id={}", plan.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        if (status == null || (status != BillingPlan.STATUS_OFF && status != BillingPlan.STATUS_ON)) {
            throw new CustomException("非法的套餐状态");
        }
        BillingPlan exist = getById(id);
        if (exist == null) {
            throw new CustomException("套餐不存在");
        }
        BillingPlan update = new BillingPlan();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        log.info("[SaaS计费] 套餐上下架 id={}, status={}", id, status);
    }

    /**
     * 校验套餐价格与额度字段合法性。
     *
     * @param plan 套餐
     */
    private void validatePlan(BillingPlan plan) {
        if (plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("月付价格不能为空且不能为负数");
        }
        if (plan.getAnnualPrice() != null && plan.getAnnualPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException("年付价格不能为负数");
        }
    }
}
