package com.reggie.module.franchise.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.franchise.mapper.FranchiseContractMapper;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.franchise.model.FranchiseContract;
import org.springframework.transaction.annotation.Transactional;
import com.reggie.module.franchise.service.FranchiseContractService;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/**
 * 加盟合同服务实现
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class FranchiseContractServiceImpl extends ServiceImpl<FranchiseContractMapper, FranchiseContract> implements
        FranchiseContractService {

    /**
     * 处理 stat contracts。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> statContracts(Long tenantId) {
        return this.baseMapper.statContracts(tenantId);
    }
}
