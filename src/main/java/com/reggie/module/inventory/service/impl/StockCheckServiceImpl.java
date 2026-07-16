package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.dto.StockCheckItemDTO;
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

    @Override
    public StockCheck createCheck(String operator, String remark) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<StockCheck> qw = new LambdaQueryWrapper<>();
        qw.likeRight(StockCheck::getCheckNo, "CK" + datePrefix);
        qw.orderByDesc(StockCheck::getCheckNo).last("LIMIT 1");
        StockCheck last = getOne(qw);
        int seq = last != null ? Integer.parseInt(last.getCheckNo().substring(10)) + 1 : 1;

        StockCheck sc = new StockCheck();
        sc.setTenantId(BaseContext.getCurrentTenantId());
        sc.setCheckNo("CK" + datePrefix + String.format("%03d", seq));
        sc.setStatus(StockCheckStatus.DRAFT.getValue());
        sc.setOperator(operator);
        sc.setRemark(remark);
        sc.setProfitLoss(BigDecimal.ZERO);
        save(sc);
        return sc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeCheck(Long checkId, List<StockCheckItemDTO> items) {
        StockCheck sc = getById(checkId);
        if (sc == null) {
            throw new CustomException("盘点单不存在");
        }
        if (!StockCheckStatus.DRAFT.getValue().equals(sc.getStatus()) && !StockCheckStatus.IN_PROGRESS.getValue().equals(sc.getStatus())) {
            throw new CustomException("盘点单状态不允许完成");
        }

        BigDecimal totalDiff = BigDecimal.ZERO;
        for (StockCheckItemDTO item : items) {
            Long materialId = item.getMaterialId();
            BigDecimal actualQty = item.getActualStock();

            Material material = materialService.getById(materialId);
            if (material == null) {
                throw new CustomException("食材不存在: " + materialId);
            }

            BigDecimal bookQty = material.getStockQty();
            BigDecimal diff = actualQty.subtract(bookQty);
            totalDiff = totalDiff.add(diff.multiply(material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO));

            material.setStockQty(actualQty);
            materialService.updateById(material);

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
            record.setUnitPrice(material.getUnitPrice());
            record.setBizId(checkId);
            record.setRemark("盘点调整");
            record.setOperator(sc.getOperator());
            stockRecordService.save(record);
        }

        sc.setStatus(StockCheckStatus.DONE.getValue());
        sc.setTotalDiffAmount(totalDiff.setScale(2, RoundingMode.HALF_UP));
        sc.setProfitLoss(totalDiff.setScale(2, RoundingMode.HALF_UP));
        updateById(sc);
    }

    @Override
    public Page<StockCheck> page(Page<StockCheck> pageInfo) {
        Page<StockCheck> result = super.page(pageInfo);
        List<StockCheck> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillStockCheckInfo(records);
        }
        return result;
    }

    @Override
    public List<StockCheck> list(LambdaQueryWrapper<StockCheck> queryWrapper) {
        List<StockCheck> list = super.list(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            fillStockCheckInfo(list);
        }
        return list;
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

                for (StockCheckDetail d : allDetails) {
                    if (d.getMaterialId() != null) {
                        d.setMaterialName(nameMap.get(d.getMaterialId()));
                    }
                }
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
