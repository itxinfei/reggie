package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.MaterialCategoryMapper;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.SupplierMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.MaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.reggie.module.inventory.model.Material.STATUS_NORMAL;

/**
 * 食材服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @Autowired
    private MaterialCategoryMapper materialCategoryMapper;
    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public Page<Material> pageWithCategory(int page, int pageSize) {
        Page<Material> pageInfo = new Page<>(page, pageSize);
        return page(pageInfo);
    }

    @Override
    public List<Material> checkWarning() {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.eq(Material::getStatus, STATUS_NORMAL);
        qw.apply("stock_qty < min_stock");
        List<Material> list = list(qw);
        fillCategoryAndSupplier(list);
        return list;
    }

    /**
     * 重写双参分页方法，确保分页查询后填充分类名称和供应商名称
     * <p>修改点：Controller 调用 materialService.page(pageInfo, qw) 时走此方法，
     * 原来因未重写双参版本导致 fillCategoryAndSupplier 被绕过，分类名/供应商名始终为空</p>
     */
    @Override
    public <E extends IPage<Material>> E page(E page, Wrapper<Material> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<Material> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillCategoryAndSupplier(records);
        }
        return result;
    }

    public Page<Material> page(Page<Material> pageInfo) {
        Page<Material> result = super.page(pageInfo);
        List<Material> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillCategoryAndSupplier(records);
        }
        return result;
    }

    @Override
    public List<Material> list(Wrapper<Material> queryWrapper) {
        List<Material> list = super.list(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            fillCategoryAndSupplier(list);
        }
        return list;
    }

    /**
     * 批量填充食材的分类名称和供应商名称
     */
    private void fillCategoryAndSupplier(List<Material> materials) {
        if (CollectionUtils.isEmpty(materials)) return;

        // 收集所有分类ID和供应商ID
        List<Long> categoryIds = materials.stream()
                .map(Material::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> supplierIds = materials.stream()
                .map(Material::getSupplierId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询分类名称
        Map<Long, String> categoryNameMap = CollectionUtils.isEmpty(categoryIds) ? null
                : materialCategoryMapper.selectList(new LambdaQueryWrapper<com.reggie.module.inventory.model.MaterialCategory>()
                        .in(com.reggie.module.inventory.model.MaterialCategory::getId, categoryIds))
                .stream().collect(Collectors.toMap(
                        com.reggie.module.inventory.model.MaterialCategory::getId,
                        com.reggie.module.inventory.model.MaterialCategory::getName,
                        (v1, v2) -> v1));

        // 批量查询供应商名称
        Map<Long, String> supplierNameMap = CollectionUtils.isEmpty(supplierIds) ? null
                : supplierMapper.selectList(new LambdaQueryWrapper<com.reggie.module.inventory.model.Supplier>()
                        .in(com.reggie.module.inventory.model.Supplier::getId, supplierIds))
                .stream().collect(Collectors.toMap(
                        com.reggie.module.inventory.model.Supplier::getId,
                        com.reggie.module.inventory.model.Supplier::getName,
                        (v1, v2) -> v1));

        // 填充关联名称
        for (Material m : materials) {
            if (categoryNameMap != null && m.getCategoryId() != null) {
                m.setCategoryName(categoryNameMap.get(m.getCategoryId()));
            }
            if (supplierNameMap != null && m.getSupplierId() != null) {
                m.setSupplierName(supplierNameMap.get(m.getSupplierId()));
            }
        }
    }
}
