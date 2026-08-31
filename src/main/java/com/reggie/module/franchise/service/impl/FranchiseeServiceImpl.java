package com.reggie.module.franchise.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.franchise.mapper.FranchiseeMapper;
import com.reggie.module.franchise.model.Franchisee;
import com.reggie.module.franchise.service.FranchiseeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 加盟商服务实现
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@Service
public class FranchiseeServiceImpl extends ServiceImpl<FranchiseeMapper, Franchisee> implements FranchiseeService {

    @Override
    public Map<String, Object> statFranchisees(Long tenantId) {
        return this.baseMapper.statFranchisees(tenantId);
    }
}
