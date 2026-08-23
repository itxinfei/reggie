package com.reggie.module.franchise.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.franchise.mapper.FranchiseContractMapper;
import com.reggie.module.franchise.model.FranchiseContract;
import com.reggie.module.franchise.service.FranchiseContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 加盟合同服务实现
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@Service
public class FranchiseContractServiceImpl extends ServiceImpl<FranchiseContractMapper, FranchiseContract> implements FranchiseContractService {
}
