package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.dining.mapper.TableAreaMapper;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.TableAreaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 桌台区域服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TableAreaServiceImpl extends ServiceImpl<TableAreaMapper, TableArea> implements TableAreaService {

    @Override
    public Page<TableArea> pageQuery(int page, int pageSize) {
        Page<TableArea> pageRequest = PageUtils.of(page, pageSize);
        return this.page(pageRequest,
                new LambdaQueryWrapper<TableArea>()
                        .eq(TableArea::getTenantId, BaseContext.getCurrentTenantId())
                        .orderByAsc(TableArea::getSort));
    }

    @Override
    public TableArea getByName(String name) {
        return this.list(new LambdaQueryWrapper<TableArea>()
                .eq(TableArea::getName, name)
                .eq(TableArea::getTenantId, BaseContext.getCurrentTenantId())
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }
}

