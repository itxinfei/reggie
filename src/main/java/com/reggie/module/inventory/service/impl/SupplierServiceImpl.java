package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    /**
     * 分页查询（死代码：SupplierController 直接调用父类 page(pageInfo, qw)，
     * 未经过此方法。保留作为备用实现，但 TenantLineInnerInterceptor 已自动添加租户过滤）
     */
    @Override
    public Page<Supplier> pageQuery(int page, int pageSize, String name) {
        Page<Supplier> pageRequest = new Page<>(page, pageSize);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                .orderByDesc(Supplier::getId);
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Supplier::getName, name);
        }
        return this.page(pageRequest, wrapper);
    }

    /**
     * 查询已启用供应商（死代码：SupplierController 自己写了过滤逻辑，未调用此方法）
     */
    @Override
    public List<Supplier> listEnabled() {
        return this.list(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getStatus, 1)
                // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                .orderByAsc(Supplier::getId));
    }
}
