package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.SupplierMapper;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.SupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 供应商服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {
}
