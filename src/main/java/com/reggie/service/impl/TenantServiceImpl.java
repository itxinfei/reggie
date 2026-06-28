package com.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.entity.Tenant;
import com.reggie.mapper.TenantMapper;
import com.reggie.service.TenantService;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
}
