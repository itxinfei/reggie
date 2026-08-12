package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.MaterialCategoryMapper;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.service.MaterialCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.reggie.module.category.model.Category;
import org.springframework.transaction.annotation.Transactional;

/**
 * 食材分类服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
/**
 * MaterialCategory service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MaterialCategoryServiceImpl extends ServiceImpl<MaterialCategoryMapper, MaterialCategory> implements MaterialCategoryService {

    /**
     * 分页查询（死代码：所有 Controller 均直接调用父类 page(pageInfo, qw)，
     * 未经过此方法。保留作为备用实现，但 TenantLineInnerInterceptor 已自动添加租户过滤）
     */
    @Override
    public Page<MaterialCategory> pageQuery(int page, int pageSize) {
        Page<MaterialCategory> pageRequest = new Page<>(page, pageSize);
        return this.page(pageRequest,
                new LambdaQueryWrapper<MaterialCategory>()
                        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                        .orderByAsc(MaterialCategory::getSort));
    }
}



