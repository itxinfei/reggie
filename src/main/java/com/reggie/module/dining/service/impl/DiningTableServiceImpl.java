package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.enums.DiningTableStatus;
import com.reggie.module.dining.mapper.DiningTableMapper;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.TableAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 堂食桌台服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {

    /** 桌台区域服务 */
    @Autowired
    private TableAreaService tableAreaService;

    /** 桌台状态流转白名单：每个状态允许的合法目标状态 */
    private static final Map<String, Set<String>> ALLOWED_TABLE_TRANSITIONS = new LinkedHashMap<>();

    static {
        // 空闲 -> 占用/预留/清洁中
        ALLOWED_TABLE_TRANSITIONS.put(DiningTableStatus.FREE.getValue(),
                new LinkedHashSet<>(Arrays.asList(
                        DiningTableStatus.OCCUPIED.getValue(),
                        DiningTableStatus.RESERVED.getValue(),
                        DiningTableStatus.CLEANING.getValue())));
        // 占用 -> 空闲/清洁中
        ALLOWED_TABLE_TRANSITIONS.put(DiningTableStatus.OCCUPIED.getValue(),
                new LinkedHashSet<>(Arrays.asList(
                        DiningTableStatus.FREE.getValue(),
                        DiningTableStatus.CLEANING.getValue())));
        // 预留 -> 占用（到店）/空闲（取消）
        ALLOWED_TABLE_TRANSITIONS.put(DiningTableStatus.RESERVED.getValue(),
                new LinkedHashSet<>(Arrays.asList(
                        DiningTableStatus.OCCUPIED.getValue(),
                        DiningTableStatus.FREE.getValue())));
        // 清洁中 -> 空闲
        ALLOWED_TABLE_TRANSITIONS.put(DiningTableStatus.CLEANING.getValue(),
                new LinkedHashSet<>(Arrays.asList(DiningTableStatus.FREE.getValue())));
    }

    @Override
    public void changeStatus(Long tableId, String status) {
        // fail-closed：强制租户校验
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }

        // 查询当前桌台（带租户隔离），用于校验状态流转合法性
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        qw.eq(DiningTable::getId, tableId)
          .eq(DiningTable::getTenantId, tenantId);
        DiningTable table = getOne(qw);
        if (table == null) {
            throw new CustomException("桌台不存在或无权操作");
        }

        // 校验状态流转合法性
        String currentStatus = table.getStatus();
        if (currentStatus != null && !currentStatus.equals(status)) {
            Set<String> allowed = ALLOWED_TABLE_TRANSITIONS.get(currentStatus);
            if (allowed == null || !allowed.contains(status)) {
                throw new CustomException("非法桌台状态流转: " + currentStatus + " -> " + status);
            }
        }

        // 原子更新：附加期望旧状态作为乐观锁条件，防止并发双重占用
        LambdaUpdateWrapper<DiningTable> uw = new LambdaUpdateWrapper<>();
        uw.eq(DiningTable::getId, tableId)
          .eq(DiningTable::getTenantId, tenantId)
          .eq(DiningTable::getStatus, currentStatus)
          .set(DiningTable::getStatus, status);
        boolean success = update(uw);
        if (!success) {
            // update 返回 false 说明状态已被其他线程变更，存在并发冲突
            throw new CustomException("桌台状态已被变更，请刷新后重试");
        }
    }

    @Override
    public Page<DiningTable> pageWithArea(int page, int pageSize) {
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(DiningTable::getSort);
        Page<DiningTable> pageInfo = new Page<>(page, pageSize);
        page(pageInfo, qw);

        // 修复 N+1：先收集本页 areaId 去重，一次性批量查询区域，构建 Map 后填充
        List<DiningTable> records = pageInfo.getRecords();
        if (records != null && !records.isEmpty()) {
            // 收集非空 areaId 并去重
            Map<Long, String> areaNameMap = new HashMap<>();
            java.util.Set<Long> areaIds = new java.util.HashSet<>();
            for (DiningTable table : records) {
                if (table.getAreaId() != null) {
                    areaIds.add(table.getAreaId());
                }
            }
            if (!areaIds.isEmpty()) {
                List<TableArea> areas = tableAreaService.listByIds(areaIds);
                for (TableArea area : areas) {
                    areaNameMap.put(area.getId(), area.getName());
                }
            }
            for (DiningTable table : records) {
                if (table.getAreaId() != null) {
                    table.setAreaName(areaNameMap.get(table.getAreaId()));
                }
            }
        }
        return pageInfo;
    }
}


