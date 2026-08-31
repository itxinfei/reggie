package com.reggie.module.franchise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.franchise.model.FranchiseContract;

import java.util.Map;

/**
 * 加盟合同服务（含抽成规则）
 *
 * @author reggie
 * @since 2026-08-15
 */
public interface FranchiseContractService extends IService<FranchiseContract> {

    /**
     * 合同统计（总数、生效、终止、关联加盟商数）
     *
     * @param tenantId 总部租户ID
     * @return 聚合结果
     */
    Map<String, Object> statContracts(Long tenantId);
}
