package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.dining.mapper.DiningTableMapper;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.TableAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 堂食桌台服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {

    /** 桌台区域服务 */
    @Autowired
    private TableAreaService tableAreaService;

    @Override
    public void changeStatus(Long tableId, String status) {
        DiningTable table = getById(tableId);
        if (table != null) {
            table.setStatus(status);
            updateById(table);
        }
    }

    @Override
    public Page<DiningTable> pageWithArea(int page, int pageSize) {
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(DiningTable::getSort);
        Page<DiningTable> pageInfo = new Page<>(page, pageSize);
        page(pageInfo, qw);
        for (DiningTable table : pageInfo.getRecords()) {
            if (table.getAreaId() != null) {
                TableArea area = tableAreaService.getById(table.getAreaId());
                if (area != null) {
                    table.setAreaName(area.getName());
                }
            }
        }
        return pageInfo;
    }
}
