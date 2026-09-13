package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.StockCheckDetailMapper;
import com.reggie.module.inventory.mapper.StockCheckMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.model.StockCheckDetail;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.enums.StockCheckStatus;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.StockCheckService;
import com.reggie.module.inventory.service.StockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存盘点服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class StockCheckServiceImpl extends ServiceImpl<StockCheckMapper, StockCheck> implements StockCheckService {

    /** 食材服务 */
    @Autowired
    private MaterialService materialService;

    /** 库存记录服务 */
    @Autowired
    private StockRecordService stockRecordService;

    /** 盘点明细Mapper */
    @Autowired
    private StockCheckDetailMapper stockCheckDetailMapper;

    /** 食材Mapper（用于原子盘点调整库存） */
    @Autowired
    private MaterialMapper materialMapper;

    /**
     * 创建 check。
     * @param operator 参数 operator
     * @param remark 参数 remark
     * @return 返回结果
     */
    @Override
    public StockCheck createCheck(String operator, String remark) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 并发下 MAX+1 生成单号可能撞号（两个请求同时读到同一 last），由唯一索引 uk_check_no 兜底：
        // 冲突时递增 seq 重试，最多 5 次，保证同一日单号唯一不重复
        int seq;
        try {
            LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
            qw.likeRight(StockCheck::getCheckNo, "CK" + datePrefix);
            qw.orderByDesc(StockCheck::getCheckNo).last("LIMIT 1");
            StockCheck last = getOne(qw);
            seq = last != null ? Integer.parseInt(last.getCheckNo().substring(10)) + 1 : 1;
        } catch (NumberFormatException e) {
            seq = Integer.MAX_VALUE;
        }
        if (seq == Integer.MAX_VALUE) {
            StockCheck sc = new StockCheck();
            sc.setTenantId(BaseContext.getCurrentTenantId());
            sc.setCheckNo("CK" + datePrefix + System.currentTimeMillis());
            // 修改点：创建盘点单时设为"草稿"状态，等待用户添加盘点项
            sc.setStatus(StockCheckStatus.DRAFT.getValue());
            sc.setOperator(operator);
            sc.setRemark(remark);
            sc.setProfitLoss(BigDecimal.ZERO);
            save(sc);
            return sc;
        }
        int attempts = 0;
        while (attempts++ < 5) {
            StockCheck sc = new StockCheck();
            sc.setTenantId(BaseContext.getCurrentTenantId());
            sc.setCheckNo("CK" + datePrefix + String.format("%03d", seq));
            // 修改点：创建盘点单时设为"草稿"状态，等待用户添加盘点项后再变为"进行中"
            sc.setStatus(StockCheckStatus.DRAFT.getValue());
            sc.setOperator(operator);
            sc.setRemark(remark);
            sc.setProfitLoss(BigDecimal.ZERO);
            try {
                save(sc);
                return sc;
            } catch (DuplicateKeyException e) {
                seq++;
            }
        }
        throw new CustomException("盘点单号生成冲突，请重试");
    }

    /**
     * 完成 check。
     * @param checkId 参数 checkId
     * @param items 参数 items
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCheck(Long checkId, List<StockCheckItemDTO> items) {
        StockCheck sc = getById(checkId);
        if (sc == null) {
            throw new CustomException("盘点单不存在");
        }
        // 租户归属校验：防止跨租户越权完成盘点并篡改库存
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(sc.getTenantId())) {
            throw new CustomException("无权操作其他租户的盘点单");
        }
        if (!StockCheckStatus.DRAFT.getValue().equals(sc.getStatus()) && !StockCheckStatus.IN_PROGRESS.getValue()
                .equals(sc.getStatus())) {
            throw new CustomException("盘点单状态不允许完成");
        }

        BigDecimal totalDiff = BigDecimal.ZERO;
        for (StockCheckItemDTO item : items) {
            totalDiff = totalDiff.add(applyCheckItem(item, checkId, sc));
        }

        // 修改点：CAS 状态更新——仅当状态仍为 DRAFT/IN_PROGRESS 时才置为 DONE，
        // 返回 0 表示已被他人完成，抛异常回滚整个事务（含库存调整/明细插入）
        BigDecimal finalDiff = totalDiff.setScale(2, RoundingMode.HALF_UP);
        LambdaUpdateWrapper<StockCheck> casUpdate = new LambdaUpdateWrapper<>();
        casUpdate.eq(StockCheck::getId, checkId)
                .in(StockCheck::getStatus, StockCheckStatus.DRAFT.getValue(), StockCheckStatus.IN_PROGRESS.getValue())
                .set(StockCheck::getStatus, StockCheckStatus.DONE.getValue())
                .set(StockCheck::getTotalDiffAmount, finalDiff);
        int updated = baseMapper.update(null, casUpdate);
        if (updated == 0) {
            throw new CustomException("盘点单已被他人完成或状态已变更");
        }
    }

    /**
     * 处理单条盘点明细：CAS 调整库存、写明细与流水，返回差异金额（等价抽取，降低方法长度）。
     */
    private BigDecimal applyCheckItem(StockCheckItemDTO item, Long checkId, StockCheck sc) {
        Long materialId = item.getMaterialId();
        BigDecimal actualQty = item.getActualStock();
        if (materialId == null || actualQty == null) {
            throw new CustomException("盘点明细数据不完整，请检查食材ID和实盘数量");
        }

        // CAS 重试：读取账面数量后尝试条件更新，若并发出入库已改变 stock_qty 则重试（最多 3 次）
        BigDecimal bookQty = null;
        BigDecimal diff = null;
        BigDecimal unitPrice = BigDecimal.ZERO;
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Material material = materialService.getById(materialId);
            if (material == null) {
                throw new CustomException("食材不存在: " + materialId);
            }
            bookQty = material.getStockQty() != null ? material.getStockQty() : BigDecimal.ZERO;
            unitPrice = material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO;
            diff = actualQty.subtract(bookQty);

            // CAS 原子更新：仅当 stock_qty 仍等于读取时的 bookQty 才写入 actualQty
            int casRows = materialMapper.casAdjustStock(materialId, bookQty, actualQty);
            if (casRows > 0) {
                // CAS 成功，跳出重试
                break;
            }
            if (attempt == maxRetries) {
                throw new CustomException("食材「" + material.getName() + "」库存并发变动频繁，盘点保存失败，请重试");
            }
            // CAS 失败，重新读取最新账面数量进入下一轮
            log.warn("[盘点CAS重试] 食材{}第{}次尝试失败，库存已变动，重新读取", String.valueOf(materialId), String.valueOf(attempt));
        }

        StockCheckDetail detail = new StockCheckDetail();
        detail.setCheckId(checkId);
        detail.setMaterialId(materialId);
        detail.setBookQty(bookQty);
        detail.setActualQty(actualQty);
        detail.setDiffQty(diff);
        detail.setDiff(diff);
        detail.setRemark(item.getRemark());
        stockCheckDetailMapper.insert(detail);

        StockRecord record = new StockRecord();
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setMaterialId(materialId);
        record.setType(StockRecordType.CHECK.getValue());
        record.setQty(diff);
        record.setUnitPrice(unitPrice);
        record.setBizId(checkId);
        record.setRemark("盘点调整");
        record.setOperator(sc.getOperator());
        stockRecordService.save(record);

        return diff.multiply(unitPrice);
    }

    /**
     * 修改点：重写带条件分页（Controller 实际调用 page(pageInfo, qw)）。IService.page 为泛型方法
     * &lt;E extends IPage&lt;T&gt;&gt; E page(E, Wrapper)，必须以相同泛型签名重写，否则经接口引用调用时走泛型父类方法、
     * 不执行 fillStockCheckInfo，导致 itemCount/profitLoss 列空白。
     */
    @Override
    public <E extends IPage<StockCheck>> E page(E page, Wrapper<StockCheck> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<StockCheck> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillStockCheckInfo(records);
        }
        return result;
    }

    /**
     * 修改点：重写无条件下分页（与带条件分页一致，确保填充 itemCount/profitLoss）。
     */
    @Override
    public <E extends IPage<StockCheck>> E page(E page) {
        E result = super.page(page);
        List<StockCheck> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillStockCheckInfo(records);
        }
        return result;
    }

    /**
     * 查询列表。
     * @param queryWrapper 参数 queryWrapper
     * @return 返回结果
     */
    public List<StockCheck> list(Wrapper<StockCheck> queryWrapper) {
        List<StockCheck> list = super.list(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            fillStockCheckInfo(list);
        }
        return list;
    }

    @Override
    public Map<String, Object> getStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
        long total = count(qw);
        qw = new LambdaQueryWrapper<>();
        qw.eq(StockCheck::getStatus, StockCheckStatus.DRAFT.getValue());
        long draft = count(qw);
        qw = new LambdaQueryWrapper<>();
        qw.eq(StockCheck::getStatus, StockCheckStatus.IN_PROGRESS.getValue());
        long inProgress = count(qw);
        qw = new LambdaQueryWrapper<>();
        qw.eq(StockCheck::getStatus, StockCheckStatus.DONE.getValue());
        long done = count(qw);
        // 差异项数：已完成盘点单中明细 diffQty != 0 的条目数
        long diffCount = 0;
        if (done > 0) {
            List<StockCheck> doneChecks = list(new LambdaQueryWrapper<StockCheck>()
                    .eq(StockCheck::getStatus, StockCheckStatus.DONE.getValue())
                    .select(StockCheck::getId));
            if (!doneChecks.isEmpty()) {
                List<Long> checkIds = doneChecks.stream().map(StockCheck::getId).collect(Collectors.toList());
                List<StockCheckDetail> details = stockCheckDetailMapper.selectList(
                        new LambdaQueryWrapper<StockCheckDetail>().in(StockCheckDetail::getCheckId, checkIds));
                diffCount = details.stream()
                        .filter(d -> d.getDiffQty() != null && d.getDiffQty().compareTo(BigDecimal.ZERO) != 0)
                        .count();
            }
        }
        java.util.LinkedHashMap<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("total", total);
        stats.put("draft", draft);
        stats.put("inProgress", inProgress);
        stats.put("done", done);
        stats.put("diffCount", diffCount);
        return stats;
    }

    @Override
    public List<StockCheckDetail> getDetails(Long checkId) {
        List<StockCheckDetail> details = stockCheckDetailMapper.selectList(
                new LambdaQueryWrapper<StockCheckDetail>().eq(StockCheckDetail::getCheckId, checkId));
        // 填充 diff 瞬态字段和食材名称
        if (!details.isEmpty()) {
            for (StockCheckDetail d : details) {
                d.setDiff(d.getDiffQty());
            }
            List<Long> materialIds = details.stream()
                    .map(StockCheckDetail::getMaterialId).filter(id -> id != null).distinct()
                    .collect(Collectors.toList());
            if (!materialIds.isEmpty()) {
                Map<Long, String> nameMap = materialService.list(
                        new LambdaQueryWrapper<Material>().in(Material::getId, materialIds))
                        .stream().collect(Collectors.toMap(Material::getId, Material::getName, (v1, v2) -> v1));
                details.forEach(d -> d.setMaterialName(nameMap.get(d.getMaterialId())));
            }
        }
        return details;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCheckItems(Long checkId, List<StockCheckItemDTO> items) {
        StockCheck sc = getById(checkId);
        if (sc == null) {
            throw new CustomException("盘点单不存在");
        }
        if (!StockCheckStatus.DRAFT.getValue().equals(sc.getStatus())
                && !StockCheckStatus.IN_PROGRESS.getValue().equals(sc.getStatus())) {
            throw new CustomException("仅草稿或进行中的盘点单可添加盘点项");
        }
        // 清除旧明细
        stockCheckDetailMapper.delete(new LambdaQueryWrapper<StockCheckDetail>()
                .eq(StockCheckDetail::getCheckId, checkId));
        // 写入新明细（账面数量从当前库存快照）
        for (StockCheckItemDTO item : items) {
            Material material = materialService.getById(item.getMaterialId());
            if (material == null) {
                throw new CustomException("食材不存在: " + item.getMaterialId());
            }
            BigDecimal bookQty = material.getStockQty() != null ? material.getStockQty() : BigDecimal.ZERO;
            StockCheckDetail detail = new StockCheckDetail();
            detail.setCheckId(checkId);
            detail.setMaterialId(item.getMaterialId());
            detail.setBookQty(bookQty);
            detail.setActualQty(BigDecimal.ZERO);
            detail.setDiffQty(BigDecimal.ZERO);
            detail.setDiff(BigDecimal.ZERO);
            stockCheckDetailMapper.insert(detail);
        }
        // 状态变为进行中
        if (StockCheckStatus.DRAFT.getValue().equals(sc.getStatus())) {
            LambdaUpdateWrapper<StockCheck> uw = new LambdaUpdateWrapper<>();
            uw.eq(StockCheck::getId, checkId)
                    .eq(StockCheck::getStatus, StockCheckStatus.DRAFT.getValue())
                    .set(StockCheck::getStatus, StockCheckStatus.IN_PROGRESS.getValue());
            baseMapper.update(null, uw);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordActualQty(Long checkId, List<StockCheckItemDTO> items) {
        StockCheck sc = getById(checkId);
        if (sc == null) {
            throw new CustomException("盘点单不存在");
        }
        if (!StockCheckStatus.IN_PROGRESS.getValue().equals(sc.getStatus())) {
            throw new CustomException("仅进行中的盘点单可录入实盘数量");
        }
        for (StockCheckItemDTO item : items) {
            if (item.getMaterialId() == null || item.getActualStock() == null) {
                continue;
            }
            StockCheckDetail existing = stockCheckDetailMapper.selectOne(
                    new LambdaQueryWrapper<StockCheckDetail>()
                            .eq(StockCheckDetail::getCheckId, checkId)
                            .eq(StockCheckDetail::getMaterialId, item.getMaterialId()));
            if (existing == null) {
                throw new CustomException("该食材不在盘点项中: " + item.getMaterialId());
            }
            BigDecimal diff = item.getActualStock().subtract(existing.getBookQty());
            existing.setActualQty(item.getActualStock());
            existing.setDiffQty(diff);
            existing.setDiff(diff);
            stockCheckDetailMapper.updateById(existing);
        }
    }

    /**
     * 填充盘点单的 itemCount、profitLoss，以及明细的 materialName 和 diff
     */
    private void fillStockCheckInfo(List<StockCheck> checks) {
        if (CollectionUtils.isEmpty(checks)) return;

        // 同步 profitLoss = totalDiffAmount
        for (StockCheck sc : checks) {
            if (sc.getProfitLoss() == null && sc.getTotalDiffAmount() != null) {
                sc.setProfitLoss(sc.getTotalDiffAmount());
            }
        }

        // 收集所有盘点单ID
        List<Long> checkIds = checks.stream()
                .map(StockCheck::getId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (checkIds.isEmpty()) return;

        // 批量查询明细并填充 materialName
        List<StockCheckDetail> allDetails = stockCheckDetailMapper.selectList(
                new LambdaQueryWrapper<StockCheckDetail>().in(StockCheckDetail::getCheckId, checkIds));

        // 同步 diff = diffQty
        for (StockCheckDetail d : allDetails) {
            d.setDiff(d.getDiffQty());
        }

        if (!CollectionUtils.isEmpty(allDetails)) {
            List<Long> materialIds = allDetails.stream()
                    .map(StockCheckDetail::getMaterialId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!materialIds.isEmpty()) {
                Map<Long, String> nameMap = materialService.list(
                        new LambdaQueryWrapper<Material>().in(Material::getId, materialIds))
                        .stream().collect(Collectors.toMap(
                                Material::getId,
                                Material::getName,
                                (v1, v2) -> v1));

                allDetails.forEach(d -> {
                    if (d.getMaterialId() != null) {
                        d.setMaterialName(nameMap.get(d.getMaterialId()));
                    }
                });
            }
        }

        // 按盘点单ID分组，设置 itemCount
        Map<Long, List<StockCheckDetail>> detailMap = allDetails.stream()
                .collect(Collectors.groupingBy(StockCheckDetail::getCheckId));

        for (StockCheck sc : checks) {
            List<StockCheckDetail> details = detailMap.get(sc.getId());
            sc.setItemCount(details != null ? details.size() : 0);
            // 将明细挂到对象上（供前端展开行使用）
            sc.setDetails(details);
        }
    }
}

