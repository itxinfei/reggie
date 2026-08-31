package com.reggie.module.franchise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.franchise.model.Franchisee;

import java.util.Map;

/**
 * 加盟商服务
 *
 * @author reggie
 * @since 2026-08-15
 */
public interface FranchiseeService extends IService<Franchisee> {

    /**
     * 加盟商统计（总数、启用、禁用、关联合同数）
     *
     * @param tenantId 总部租户ID
     * @return 聚合结果
     */
    Map<String, Object> statFranchisees(Long tenantId);
}
