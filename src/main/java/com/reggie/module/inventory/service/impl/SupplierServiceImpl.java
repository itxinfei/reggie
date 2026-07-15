package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.inventory.mapper.SupplierMapper;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.SupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 供应商服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Service
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    @Override
    public Page<Supplier> pageQuery(int page, int pageSize, String name) {
        Page<Supplier> pageRequest = new Page<>(page, pageSize);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(Supplier::getId);
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Supplier::getName, name);
        }
        return this.page(pageRequest, wrapper);
    }

    @Override
    public List<Supplier> listEnabled() {
        return this.list(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getStatus, 1)
                .eq(Supplier::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(Supplier::getId));
    }
}
