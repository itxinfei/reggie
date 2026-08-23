package com.reggie.module.franchise.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.franchise.mapper.FranchiseSettlementMapper;
import com.reggie.module.franchise.model.FranchiseContract;
import com.reggie.module.franchise.model.FranchiseSettlement;
import com.reggie.module.franchise.service.FranchiseContractService;
import com.reggie.module.franchise.service.FranchiseSettlementService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;

/**
 * 加盟分账结算单服务实现
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@Service
public class FranchiseSettlementServiceImpl extends ServiceImpl<FranchiseSettlementMapper, FranchiseSettlement>
        implements FranchiseSettlementService {

    /** 加盟合同服务 */
    @Autowired
    private FranchiseContractService franchiseContractService;

    /** 订单 Mapper（跨租户聚合加盟店营业额，需绕过租户拦截器） */
    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FranchiseSettlement generateSettlement(Long contractId, String settlePeriod) {
        FranchiseContract contract = franchiseContractService.getById(contractId);
        if (contract == null) {
            throw new CustomException("加盟合同不存在");
        }
        // 租户归属校验：加盟合同必须属于当前租户
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(contract.getTenantId())) {
            throw new CustomException("无权操作其他租户的加盟合同");
        }
        if (contract.getStatus() == null || contract.getStatus() != FranchiseContract.STATUS_ACTIVE) {
            throw new CustomException("加盟合同未生效，无法生成结算单");
        }
        if (settlePeriod == null || settlePeriod.trim().isEmpty()) {
            throw new CustomException("结算周期不能为空");
        }
        String period = settlePeriod.trim();

        // 幂等：同一合同+周期已存在则直接返回（uk_settle_contract_period 兜底）
        FranchiseSettlement exist = lambdaQuery()
                .eq(FranchiseSettlement::getContractId, contractId)
                .eq(FranchiseSettlement::getSettlePeriod, period)
                .one();
        if (exist != null) {
            log.info("[加盟分账] 结算单已存在，幂等返回: contractId={}, period={}", contractId, period);
            return exist;
        }

        // 聚合周期内已完成订单（status=4），按门店租户过滤
        // 注意：总部租户聚合加盟门店订单属跨租户场景，必须走 @InterceptorIgnore 的原生 SQL，
        // 否则 TenantLineInnerInterceptor 会追加总部 tenant_id 条件导致查不到加盟店数据。
        YearMonth ym = YearMonth.parse(period);
        LocalDate begin = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Long storeTenantId = contract.getStoreTenantId();
        if (storeTenantId == null) {
            throw new CustomException("加盟合同未关联门店（storeTenantId 为空），无法生成结算单");
        }
        Map<String, Object> agg = orderMapper.sumOrdersByTenantAndPeriod(
                storeTenantId,
                Orders.STATUS_COMPLETED,
                begin.atStartOfDay(),
                end.atTime(java.time.LocalTime.MAX));

        BigDecimal salesAmount = toBigDecimal(getAggValue(agg, "amt"));
        int orderCount = toInt(getAggValue(agg, "cnt"));

        // 按合同抽成规则计算应抽成金额
        BigDecimal commission = calcCommission(contract, salesAmount);

        FranchiseSettlement st = new FranchiseSettlement();
        st.setTenantId(BaseContext.getCurrentTenantId());
        st.setContractId(contractId);
        st.setFranchiseeId(contract.getFranchiseeId());
        st.setStoreTenantId(contract.getStoreTenantId());
        st.setSettlePeriod(period);
        st.setOrderCount(orderCount);
        st.setSalesAmount(salesAmount.setScale(2, RoundingMode.HALF_UP));
        st.setCommissionType(contract.getCommissionType());
        st.setCommissionRate(contract.getCommissionRate());
        st.setCommissionAmount(commission.setScale(2, RoundingMode.HALF_UP));
        st.setSettleAmount(salesAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP));
        st.setStatus(FranchiseSettlement.STATUS_PENDING);
        this.save(st);
        log.info("[加盟分账] 生成结算单: contractId={}, period={}, sales={}, commission={}, settle={}",
                contractId, period, st.getSalesAmount(), st.getCommissionAmount(), st.getSettleAmount());
        return st;
    }

    /** 依据合同抽成规则计算应抽成金额 */
    private BigDecimal calcCommission(FranchiseContract contract, BigDecimal salesAmount) {
        if (contract.getCommissionType() == null) {
            return BigDecimal.ZERO;
        }
        if (contract.getCommissionType() == FranchiseContract.COMMISSION_TYPE_RATE) {
            BigDecimal rate = contract.getCommissionRate() != null ? contract.getCommissionRate() : BigDecimal.ZERO;
            return salesAmount.multiply(rate);
        }
        // 固定金额/周期
        return contract.getCommissionAmount() != null ? contract.getCommissionAmount() : BigDecimal.ZERO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSettlement(Long id) {
        FranchiseSettlement st = getById(id);
        if (st == null) {
            throw new CustomException("结算单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(st.getTenantId())) {
            throw new CustomException("无权操作其他租户的结算单");
        }
        if (st.getStatus() != FranchiseSettlement.STATUS_PENDING) {
            throw new CustomException("仅待确认状态的结算单可确认");
        }
        st.setStatus(FranchiseSettlement.STATUS_CONFIRMED);
        st.setConfirmTime(LocalDateTime.now());
        updateById(st);
        log.info("[加盟分账] 确认结算单: id={}, period={}", id, st.getSettlePeriod());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleSettlement(Long id) {
        FranchiseSettlement st = getById(id);
        if (st == null) {
            throw new CustomException("结算单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(st.getTenantId())) {
            throw new CustomException("无权操作其他租户的结算单");
        }
        if (st.getStatus() != FranchiseSettlement.STATUS_CONFIRMED) {
            throw new CustomException("仅已确认状态的结算单可结算");
        }
        st.setStatus(FranchiseSettlement.STATUS_SETTLED);
        st.setSettleTime(LocalDateTime.now());
        updateById(st);
        log.info("[加盟分账] 完成结算: id={}, period={}", id, st.getSettlePeriod());
    }

    @Override
    public Page<FranchiseSettlement> pageQuery(int page, int pageSize, String settlePeriod, Integer status, Long franchiseeId) {
        Page<FranchiseSettlement> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<FranchiseSettlement> qw = new LambdaQueryWrapper<>();
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null) {
            qw.eq(FranchiseSettlement::getTenantId, currentTenantId);
        }
        qw.eq(settlePeriod != null && !settlePeriod.trim().isEmpty(), FranchiseSettlement::getSettlePeriod, settlePeriod);
        qw.eq(status != null, FranchiseSettlement::getStatus, status);
        qw.eq(franchiseeId != null, FranchiseSettlement::getFranchiseeId, franchiseeId);
        qw.orderByDesc(FranchiseSettlement::getCreateTime);
        this.page(pageInfo, qw);
        return pageInfo;
    }

    /** 将聚合查询结果安全转为 BigDecimal（null/Number/String 均兼容） */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }

    /** 将聚合查询结果安全转为 int（null/Number/String 均兼容） */
    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 从聚合结果 Map 中取值，兼容列别名大小写。
     * H2 返回的列标签为大写（CNT/AMT），MySQL 返回原样（cnt/amt），
     * 统一做大小写兜底避免取不到值。
     */
    private Object getAggValue(Map<String, Object> agg, String key) {
        if (agg == null || key == null) {
            return null;
        }
        if (agg.containsKey(key)) {
            return agg.get(key);
        }
        return agg.get(key.toUpperCase());
    }
}
