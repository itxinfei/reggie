package com.reggie.module.franchise.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.franchise.model.FranchiseSettlement;

/**
 * 加盟分账结算单服务
 *
 * @author reggie
 * @since 2026-08-15
 */
public interface FranchiseSettlementService extends IService<FranchiseSettlement> {

    /**
     * 生成指定周期（如 2026-08）的加盟分账结算单。
     * <p>
     * 流程：取该周期内所有已完成订单（status=4），按门店租户聚合营业额与订单数；
     * 依据合同抽成规则计算应抽成金额与加盟商应结算金额；
     * 同一合同+周期唯一（uk_settle_contract_period），重复生成时幂等返回已有单据。
     * </p>
     *
     * @param contractId 加盟合同ID
     * @param settlePeriod 结算周期，如 2026-08
     * @return 生成的结算单
     */
    FranchiseSettlement generateSettlement(Long contractId, String settlePeriod);

    /**
     * 确认结算单（待确认 → 已确认）
     *
     * @param id 结算单ID
     */
    void confirmSettlement(Long id);

    /**
     * 完成结算（已确认 → 已结算）
     *
     * @param id 结算单ID
     */
    void settleSettlement(Long id);

    /**
     * 分页查询结算单，支持按周期/状态/加盟商筛选
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param settlePeriod 结算周期（可选）
     * @param status 状态（可选）
     * @param franchiseeId 加盟商ID（可选）
     * @return 分页结果
     */
    Page<FranchiseSettlement> pageQuery(int page, int pageSize, String settlePeriod, Integer status, Long franchiseeId);
}
