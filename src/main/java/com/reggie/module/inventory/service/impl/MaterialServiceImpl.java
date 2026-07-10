package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.service.MaterialService;
import static com.reggie.module.inventory.model.Material.STATUS_NORMAL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 食材服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

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
        return list(qw);
    }
}
