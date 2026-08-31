package com.reggie.module.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.enums.DiningTableStatus;
import com.reggie.enums.OrderStatus;
import com.reggie.module.dining.dto.MergeTableDTO;
import com.reggie.module.dining.dto.OpenTableDTO;
import com.reggie.module.dining.dto.SplitBillDTO;
import com.reggie.module.dining.dto.TransferTableDTO;
import com.reggie.module.dining.mapper.DiningTableMapper;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.TableArea;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.TableAreaService;
import com.reggie.module.dining.vo.TableStatsVO;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 堂食桌台服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {

    /** 桌台区域服务 */
    @Autowired
    private TableAreaService tableAreaService;

    /** 订单服务 */
    @Autowired
    private OrderService orderService;

    @Autowired
    private DiningTableMapper diningTableMapper;

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
        return pageWithArea(page, pageSize, null, null, null);
    }

    @Override
    public Page<DiningTable> pageWithArea(int page, int pageSize, String name, Long areaId, String status) {
        LambdaQueryWrapper<DiningTable> qw = new LambdaQueryWrapper<>();
        qw.eq(name != null && !name.isEmpty(), DiningTable::getName, name);
        qw.eq(areaId != null, DiningTable::getAreaId, areaId);
        qw.eq(status != null && !status.isEmpty(), DiningTable::getStatus, status);
        qw.orderByAsc(DiningTable::getSort);
        Page<DiningTable> pageInfo = PageUtils.of(page, pageSize);
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

    /**
     * 桌台统计：按状态分类计数
     * 使用 LambdaQueryWrapper + count() 单次查询各状态数量，租户隔离
     */
    @Override
    public TableStatsVO tableStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        TableStatsVO stats = new TableStatsVO();

        // 总数
        long total = lambdaQuery()
                .eq(DiningTable::getTenantId, tenantId)
                .count();
        stats.setTotalTables(total);

        // 各状态计数
        stats.setFreeTables(Long.valueOf(lambdaQuery()
                .eq(DiningTable::getTenantId, tenantId)
                .eq(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
                .count()));
        stats.setOccupiedTables(Long.valueOf(lambdaQuery()
                .eq(DiningTable::getTenantId, tenantId)
                .eq(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
                .count()));
        stats.setReservedTables(Long.valueOf(lambdaQuery()
                .eq(DiningTable::getTenantId, tenantId)
                .eq(DiningTable::getStatus, DiningTableStatus.RESERVED.getValue())
                .count()));
        stats.setCleaningTables(Long.valueOf(lambdaQuery()
                .eq(DiningTable::getTenantId, tenantId)
                .eq(DiningTable::getStatus, DiningTableStatus.CLEANING.getValue())
                .count()));

        return stats;
    }

    @Override
    public Map<String, Object> areaStats() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> byAreaList = diningTableMapper.statByArea();
        int total = 0;
        String maxArea = "-";
        int maxCount = 0;
        for (Map<String, Object> row : byAreaList) {
            int cnt = 0;
            Object cntVal = row.get("cnt");
            if (cntVal instanceof Number) {
                cnt = ((Number) cntVal).intValue();
            }
            total += cnt;
            if (cnt > maxCount) {
                maxCount = cnt;
                Object areaName = row.get("areaName");
                maxArea = (areaName == null ? "未知区域" : String.valueOf(areaName)) + "(" + cnt + "桌)";
            }
        }
        result.put("totalTables", total);
        result.put("maxTablesArea", maxArea);
        return result;
    }

    /**
     * 开台：将桌台状态改为占用，并绑定订单
     * <p>
     * 流程：
     * 1. 校验桌台存在且属于当前租户
     * 2. 校验桌台状态为空闲（FREE），否则不允许开台
     * 3. 校验订单存在且属于当前租户，状态为初始态（未支付/待接单）
     * 4. 更新桌台：状态 → OCCUPIED，绑定 currentOrderId
     * 5. 更新订单：绑定 tableId
     * </p>
     *
     * @param dto 开台请求
     */
    @Override
    public void openTable(OpenTableDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }

        // 1. 校验桌台
        LambdaQueryWrapper<DiningTable> tableQw = new LambdaQueryWrapper<>();
        tableQw.eq(DiningTable::getId, dto.getTableId())
               .eq(DiningTable::getTenantId, tenantId);
        DiningTable table = getOne(tableQw);
        if (table == null) {
            throw new CustomException("桌台不存在或无权操作");
        }

        // 2. 校验桌台状态必须为空闲
        if (!DiningTableStatus.FREE.getValue().equals(table.getStatus())) {
            throw new CustomException("桌台当前状态为[" + table.getStatus() + "]，无法开台");
        }

        // 3. 校验订单
        Orders order = orderService.getById(dto.getOrderId());
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (!tenantId.equals(order.getTenantId())) {
            throw new CustomException("订单不属于当前租户");
        }
        // 只允许初始态订单开台（待付款状态，value=1）
        if (order.getStatus() != null && !order.getStatus().equals(OrderStatus.PENDING_PAYMENT.getValue())) {
            throw new CustomException("订单状态为[" + order.getStatus() + "]，无法开台");
        }

        // 4. 更新桌台状态为占用，绑定订单
        LambdaUpdateWrapper<DiningTable> tableUw = new LambdaUpdateWrapper<>();
        tableUw.eq(DiningTable::getId, dto.getTableId())
               .eq(DiningTable::getTenantId, tenantId)
               .eq(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
               .set(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
               .set(DiningTable::getCurrentOrderId, dto.getOrderId());
        boolean tableOk = update(tableUw);
        if (!tableOk) {
            throw new CustomException("桌台状态已被变更，请刷新后重试");
        }

        // 5. 更新订单绑定桌台
        LambdaUpdateWrapper<Orders> orderUw = new LambdaUpdateWrapper<>();
        orderUw.eq(Orders::getId, dto.getOrderId())
               .eq(Orders::getTenantId, tenantId)
               .set(Orders::getTableId, dto.getTableId());
        orderService.update(orderUw);

        log.info("开台成功: tableId={}, orderId={}", dto.getTableId(), dto.getOrderId());
    }

    /**
     * 转台：订单从原桌台迁移到新桌台
     * <p>
     * 流程：
     * 1. 校验原桌台存在且为占用状态，且有绑定订单
     * 2. 校验目标桌台存在且为空闲状态
     * 3. 将原桌台状态改为空闲，清空 currentOrderId
     * 4. 将目标桌台状态改为占用，绑定同一订单
     * </p>
     *
     * @param dto 转台请求
     */
    @Override
    public void transferTable(TransferTableDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }

        // 1. 校验原桌台
        LambdaQueryWrapper<DiningTable> fromQw = new LambdaQueryWrapper<>();
        fromQw.eq(DiningTable::getId, dto.getFromTableId())
              .eq(DiningTable::getTenantId, tenantId);
        DiningTable fromTable = getOne(fromQw);
        if (fromTable == null) {
            throw new CustomException("原桌台不存在或无权操作");
        }
        if (!DiningTableStatus.OCCUPIED.getValue().equals(fromTable.getStatus())) {
            throw new CustomException("原桌台当前状态为[" + fromTable.getStatus() + "]，无法转台");
        }
        if (fromTable.getCurrentOrderId() == null) {
            throw new CustomException("原桌台未绑定订单，无法转台");
        }

        // 2. 校验目标桌台
        LambdaQueryWrapper<DiningTable> toQw = new LambdaQueryWrapper<>();
        toQw.eq(DiningTable::getId, dto.getToTableId())
            .eq(DiningTable::getTenantId, tenantId);
        DiningTable toTable = getOne(toQw);
        if (toTable == null) {
            throw new CustomException("目标桌台不存在或无权操作");
        }
        if (!DiningTableStatus.FREE.getValue().equals(toTable.getStatus())) {
            throw new CustomException("目标桌台当前状态为[" + toTable.getStatus() + "]，无法转入");
        }

        Long orderId = fromTable.getCurrentOrderId();

        // 3. 原桌台 → 空闲，清空绑定
        LambdaUpdateWrapper<DiningTable> fromUw = new LambdaUpdateWrapper<>();
        fromUw.eq(DiningTable::getId, dto.getFromTableId())
              .eq(DiningTable::getTenantId, tenantId)
              .eq(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
              .set(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
              .set(DiningTable::getCurrentOrderId, null);
        boolean fromOk = update(fromUw);
        if (!fromOk) {
            throw new CustomException("原桌台状态已被变更，请刷新后重试");
        }

        // 4. 目标桌台 → 占用，绑定订单
        LambdaUpdateWrapper<DiningTable> toUw = new LambdaUpdateWrapper<>();
        toUw.eq(DiningTable::getId, dto.getToTableId())
            .eq(DiningTable::getTenantId, tenantId)
            .eq(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
            .set(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
            .set(DiningTable::getCurrentOrderId, orderId);
        boolean toOk = update(toUw);
        if (!toOk) {
            throw new CustomException("目标桌台状态已被变更，请刷新后重试");
        }

        // 5. 更新订单的 tableId
        LambdaUpdateWrapper<Orders> orderUw = new LambdaUpdateWrapper<>();
        orderUw.eq(Orders::getId, orderId)
               .eq(Orders::getTenantId, tenantId)
               .set(Orders::getTableId, dto.getToTableId());
        orderService.update(orderUw);

        log.info("转台成功: fromTableId={}, toTableId={}, orderId={}", dto.getFromTableId(), dto.getToTableId(), orderId);
    }

    /**
     * 并台：将多个桌台的订单合并到主桌台
     * <p>流程：
     * 1. 校验主桌台存在且为占用状态，有绑定订单；
     * 2. 校验每个被合并桌台存在且为占用状态，有绑定订单；
     * 3. 将被合并桌台的订单转移到主桌台（更新订单的 tableId）；
     * 4. 释放被合并桌台（状态改为空闲，清空 currentOrderId）。
     * </p>
     *
     * @param dto 并台请求
     */
    @Override
    public void mergeTables(MergeTableDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }

        // 1. 校验主桌台
        LambdaQueryWrapper<DiningTable> masterQw = new LambdaQueryWrapper<>();
        masterQw.eq(DiningTable::getId, dto.getMasterTableId())
                .eq(DiningTable::getTenantId, tenantId);
        DiningTable masterTable = getOne(masterQw);
        if (masterTable == null) {
            throw new CustomException("主桌台不存在或无权操作");
        }
        if (!DiningTableStatus.OCCUPIED.getValue().equals(masterTable.getStatus())) {
            throw new CustomException("主桌台当前状态为[" + masterTable.getStatus() + "]，无法并台");
        }
        if (masterTable.getCurrentOrderId() == null) {
            throw new CustomException("主桌台未绑定订单，无法并台");
        }

        // 2. 收集被合并桌台的订单 ID
        List<Long> mergeOrderIds = new java.util.ArrayList<>();
        for (Long tableId : dto.getMergeTableIds()) {
            LambdaQueryWrapper<DiningTable> mergeQw = new LambdaQueryWrapper<>();
            mergeQw.eq(DiningTable::getId, tableId)
                   .eq(DiningTable::getTenantId, tenantId);
            DiningTable mergeTable = getOne(mergeQw);
            if (mergeTable == null) {
                throw new CustomException("桌台ID=" + tableId + "不存在或无权操作");
            }
            if (!DiningTableStatus.OCCUPIED.getValue().equals(mergeTable.getStatus())) {
                throw new CustomException("桌台ID=" + tableId + "当前状态为[" + mergeTable.getStatus() + "]，无法并台");
            }
            if (mergeTable.getCurrentOrderId() == null) {
                throw new CustomException("桌台ID=" + tableId + "未绑定订单，无法并台");
            }
            mergeOrderIds.add(mergeTable.getCurrentOrderId());
        }

        // 3. 将被合并桌台的订单转移到主桌台
        for (Long orderId : mergeOrderIds) {
            LambdaUpdateWrapper<Orders> orderUw = new LambdaUpdateWrapper<>();
            orderUw.eq(Orders::getId, orderId)
                   .eq(Orders::getTenantId, tenantId)
                   .set(Orders::getTableId, dto.getMasterTableId());
            orderService.update(orderUw);
        }

        // 4. 释放被合并桌台
        for (Long tableId : dto.getMergeTableIds()) {
            LambdaUpdateWrapper<DiningTable> tableUw = new LambdaUpdateWrapper<>();
            tableUw.eq(DiningTable::getId, tableId)
                   .eq(DiningTable::getTenantId, tenantId)
                   .eq(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
                   .set(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
                   .set(DiningTable::getCurrentOrderId, null);
            boolean ok = update(tableUw);
            if (!ok) {
                throw new CustomException("桌台ID=" + tableId + "状态已被变更，请刷新后重试");
            }
        }

        log.info("并台成功: masterTableId={}, mergeTableIds={}, orderId={}",
                dto.getMasterTableId(), dto.getMergeTableIds(), masterTable.getCurrentOrderId());
    }

    /**
     * 拆台：将桌台的订单拆分到新桌台
     * <p>流程：
     * 1. 校验原桌台存在且为占用状态；
     * 2. 校验新桌台存在且为空闲状态；
     * 3. 将指定订单从原桌台转移到新桌台；
     * 4. 原桌台若还有订单则保持占用，否则释放。
     * </p>
     *
     * @param originalTableId 原桌台 ID
     * @param newTableId      新桌台 ID
     * @param splitOrderIds   需要拆分出去的订单 ID 列表
     */
    @Override
    public void splitTable(Long originalTableId, Long newTableId, List<Long> splitOrderIds) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }
        if (splitOrderIds == null || splitOrderIds.isEmpty()) {
            throw new CustomException("拆分订单列表不能为空");
        }

        // 1. 校验原桌台
        LambdaQueryWrapper<DiningTable> originalQw = new LambdaQueryWrapper<>();
        originalQw.eq(DiningTable::getId, originalTableId)
                  .eq(DiningTable::getTenantId, tenantId);
        DiningTable originalTable = getOne(originalQw);
        if (originalTable == null) {
            throw new CustomException("原桌台不存在或无权操作");
        }
        if (!DiningTableStatus.OCCUPIED.getValue().equals(originalTable.getStatus())) {
            throw new CustomException("原桌台当前状态为[" + originalTable.getStatus() + "]，无法拆台");
        }

        // 2. 校验新桌台
        LambdaQueryWrapper<DiningTable> newQw = new LambdaQueryWrapper<>();
        newQw.eq(DiningTable::getId, newTableId)
             .eq(DiningTable::getTenantId, tenantId);
        DiningTable newTable = getOne(newQw);
        if (newTable == null) {
            throw new CustomException("新桌台不存在或无权操作");
        }
        if (!DiningTableStatus.FREE.getValue().equals(newTable.getStatus())) {
            throw new CustomException("新桌台当前状态为[" + newTable.getStatus() + "]，无法转入");
        }

        // 3. 将指定订单转移到新桌台
        for (Long orderId : splitOrderIds) {
            LambdaUpdateWrapper<Orders> orderUw = new LambdaUpdateWrapper<>();
            orderUw.eq(Orders::getId, orderId)
                   .eq(Orders::getTenantId, tenantId)
                   .set(Orders::getTableId, newTableId);
            orderService.update(orderUw);
        }

        // 4. 检查原桌台是否还有订单
        Long remainingOrderId = null;
        LambdaQueryWrapper<DiningTable> checkQw = new LambdaQueryWrapper<>();
        checkQw.eq(DiningTable::getTenantId, tenantId)
               .eq(DiningTable::getId, originalTableId);
        // 直接查询原桌台是否有绑定订单（currentOrderId 字段）
        // 注意：一个桌台可能有多个订单，需要查询所有绑定到原桌台的订单
        List<Orders> remainingOrders = orderService.list(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getTableId, originalTableId)
                        .eq(Orders::getTenantId, tenantId));
        if (remainingOrders.isEmpty()) {
            // 原桌台已无订单，释放
            LambdaUpdateWrapper<DiningTable> originalUw = new LambdaUpdateWrapper<>();
            originalUw.eq(DiningTable::getId, originalTableId)
                      .eq(DiningTable::getTenantId, tenantId)
                      .eq(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
                      .set(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
                      .set(DiningTable::getCurrentOrderId, null);
            boolean ok = update(originalUw);
            if (!ok) {
                throw new CustomException("原桌台状态已被变更，请刷新后重试");
            }
        } else {
            // 原桌台还有订单，保持占用，更新 currentOrderId 为第一个剩余订单
            remainingOrderId = remainingOrders.get(0).getId();
            LambdaUpdateWrapper<DiningTable> originalUw = new LambdaUpdateWrapper<>();
            originalUw.eq(DiningTable::getId, originalTableId)
                      .eq(DiningTable::getTenantId, tenantId)
                      .eq(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
                      .set(DiningTable::getCurrentOrderId, remainingOrderId);
            boolean ok = update(originalUw);
            if (!ok) {
                throw new CustomException("原桌台状态已被变更，请刷新后重试");
            }
        }

        // 5. 新桌台绑定第一个转移的订单
        Long firstOrderId = splitOrderIds.get(0);
        LambdaUpdateWrapper<DiningTable> newUw = new LambdaUpdateWrapper<>();
        newUw.eq(DiningTable::getId, newTableId)
             .eq(DiningTable::getTenantId, tenantId)
             .eq(DiningTable::getStatus, DiningTableStatus.FREE.getValue())
             .set(DiningTable::getStatus, DiningTableStatus.OCCUPIED.getValue())
             .set(DiningTable::getCurrentOrderId, firstOrderId);
        boolean ok = update(newUw);
        if (!ok) {
            throw new CustomException("新桌台状态已被变更，请刷新后重试");
        }

        log.info("拆台成功: originalTableId={}, newTableId={}, splitOrderIds={}",
                originalTableId, newTableId, splitOrderIds);
    }

    /**
     * AA 分账：为指定订单创建拆分子单，支持按份数均分
     * <p>主单状态变更为 SPLIT，各子单独立结算。子单金额为原单均分（保留小数）。
     * 此实现为 P1 骨架，后续可升级为按菜品明细拆分或自定义金额。</p>
     *
     * @param dto 分账请求
     */
    @Override
    public void splitBill(SplitBillDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("无操作权限，租户上下文缺失");
        }
        if (dto.getOrderId() == null || dto.getParts() == null || dto.getParts() <= 0) {
            throw new CustomException("订单ID和分账份数不能为空");
        }

        // 1. 校验主订单
        Orders masterOrder = orderService.getById(dto.getOrderId());
        if (masterOrder == null) {
            throw new CustomException("订单不存在");
        }
        if (!tenantId.equals(masterOrder.getTenantId())) {
            throw new CustomException("无权操作该订单");
        }
        // 仅未结账订单可分账（待付款 1/待接单 2/配送中 3）
        Integer status = masterOrder.getStatus();
        if (status == null
                || (!status.equals(OrderStatus.PENDING_PAYMENT.getValue())
                    && !status.equals(OrderStatus.ORDERED.getValue())
                    && !status.equals(OrderStatus.DELIVERING.getValue()))) {
            throw new CustomException("仅待付款/待接单/配送中订单可分账");
        }

        // 2. 检查是否已分账（幂等）
        long splitCount = orderService.count(
                new LambdaQueryWrapper<Orders>()
                        .eq(Orders::getMasterOrderId, masterOrder.getId())
                        .eq(Orders::getTenantId, tenantId));
        if (splitCount > 0) {
            throw new CustomException("该订单已分账，请勿重复操作");
        }

        // 3. 计算每份金额（均分）
        BigDecimal amount = masterOrder.getAmount() != null ? masterOrder.getAmount() : BigDecimal.ZERO;
        BigDecimal partAmount = amount.divide(BigDecimal.valueOf(dto.getParts()), 2, java.math.RoundingMode.HALF_UP);

        // 4. 创建子订单
        List<Orders> subOrders = new java.util.ArrayList<>();
        for (int i = 0; i < dto.getParts(); i++) {
            Orders subOrder = new Orders();
            subOrder.setNumber(masterOrder.getNumber() + "-P" + (i + 1));
            subOrder.setStatus(OrderStatus.PENDING_PAYMENT.getValue());
            subOrder.setAmount(partAmount);
            subOrder.setSource(masterOrder.getSource());
            subOrder.setTableName(masterOrder.getTableName());
            subOrder.setCustomerCount(masterOrder.getCustomerCount());
            subOrder.setRemark(dto.getRemark());
            subOrder.setOrderTime(java.time.LocalDateTime.now());
            subOrder.setTenantId(tenantId);
            subOrder.setTableId(masterOrder.getTableId());
            subOrder.setMasterOrderId(masterOrder.getId());
            subOrder.setUserId(masterOrder.getUserId());
            subOrder.setUserName(masterOrder.getUserName());
            subOrder.setConsignee(masterOrder.getConsignee());
            subOrder.setPhone(masterOrder.getPhone());
            subOrder.setAddress(masterOrder.getAddress());
            orderService.save(subOrder);
            subOrders.add(subOrder);
        }

        // 5. 主订单状态改为已分账（SPLIT）
        LambdaUpdateWrapper<Orders> masterUw = new LambdaUpdateWrapper<>();
        masterUw.eq(Orders::getId, masterOrder.getId())
                .eq(Orders::getTenantId, tenantId)
                .set(Orders::getStatus, OrderStatus.SPLIT.getValue())
                .set(Orders::getSplitCount, dto.getParts())
                .set(Orders::getRemark,
                        (masterOrder.getRemark() != null ? masterOrder.getRemark() + "; " : "")
                                + "AA分账" + dto.getParts() + "份");
        orderService.update(masterUw);

        log.info("AA 分账成功: masterOrderId={}, parts={}, subOrderIds={}",
                masterOrder.getId(), dto.getParts(),
                subOrders.stream().map(Orders::getId).collect(java.util.stream.Collectors.toList()));
    }
}


