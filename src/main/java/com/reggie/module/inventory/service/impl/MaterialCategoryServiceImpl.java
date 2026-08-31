package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.inventory.mapper.MaterialCategoryMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.service.MaterialCategoryService;
import com.reggie.module.inventory.service.MaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 食材分类服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory> implements MaterialCategoryService {

    /** 食材服务：用于统计每个分类下的食材数量 */
    @Autowired
    private MaterialService materialService;

    /**
     * 修改点：重写带条件分页（Controller 实际调用 page(pageInfo, qw)），分页完成后回填每个分类的食材数量 materialCount。
     * 分类列表接口仅返回分类实体，不含食材数，需按 category_id 聚合统计后回填，否则 materialCount 列空白。
     * IService.page 为泛型方法 <E extends IPage<T>>，子类必须以相同泛型签名重写，否则擦除冲突。
     */
    @Override
    public <E extends IPage<MaterialCategory>> E page(E page, Wrapper<MaterialCategory> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<MaterialCategory> records = result.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return result;
        }
        // 收集分页分类 ID，一次查询统计各分类下的食材数量（@TableLogic 自动过滤已删除食材）
        List<Long> categoryIds = records.stream().map(MaterialCategory::getId).collect(Collectors.toList());
        List<Material> materials = materialService.list(
                new LambdaQueryWrapper<Material>().in(Material::getCategoryId, categoryIds)
                        .select(Material::getCategoryId));
        Map<Long, Long> countMap = materials.stream()
                .collect(Collectors.groupingBy(Material::getCategoryId, Collectors.counting()));
        for (MaterialCategory category : records) {
            Long count = countMap.get(category.getId());
            category.setMaterialCount(count == null ? 0 : count.intValue());
        }
        return result;
    }

    /**
     * 分页查询（兼容旧调用）：委托给重写后的 page(E, Wrapper)，同样回填 materialCount。
     */
    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<MaterialCategory> pageQuery(int page, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<MaterialCategory> pageRequest = PageUtils.of(page, pageSize);
        return this.page(pageRequest,
                new LambdaQueryWrapper<MaterialCategory>()
                        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                        .orderByAsc(MaterialCategory::getSort));
    }
}
