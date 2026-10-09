package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.model.StockCheckDetail;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 库存盘点服务接口
 * </p>
 * <p>提供盘点单创建、盘点项设置、实盘录入、完成确认等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface StockCheckService extends IService<StockCheck> {

    /**
     * 创建盘点单（草稿状态）
     *
     * @param operator 操作人
     * @param remark   备注
     * @return 盘点单
     */
    StockCheck createCheck(String operator, String remark);

    /**
     * 创建盘点单并附带凭证图片。
     * JDK8 接口默认方法：复用旧创建逻辑后回填图片，避免破坏既有内部调用点。
     *
     * @param voucherImages 凭证图片（逗号分隔，可为 null）
     */
    default StockCheck createCheck(String operator, String remark, String voucherImages) {
        StockCheck sc = createCheck(operator, remark);
        if (voucherImages != null && !voucherImages.trim().isEmpty()) {
            sc.setVoucherImages(voucherImages);
            updateById(sc);
        }
        return sc;
    }

    /**
     * 完成盘点（提交盘点结果并调整库存）
     *
     * @param checkId 盘点单ID
     * @param items   盘点明细列表
     */
    void completeCheck(Long checkId, List<StockCheckItemDTO> items);

    /**
     * 获取盘点统计（总数/草稿/进行中/已完成/差异项数）
     *
     * @return 统计数据 Map
     */
    Map<String, Object> getStats();

    /**
     * 获取盘点单明细列表
     *
     * @param checkId 盘点单ID
     * @return 明细列表（含食材名称）
     */
    List<StockCheckDetail> getDetails(Long checkId);

    /**
     * 设置盘点项（食材列表 + 账面数量快照），盘点单从 DRAFT 变为 IN_PROGRESS
     *
     * @param checkId 盘点单ID
     * @param items   食材列表 [{materialId}]
     */
    void setCheckItems(Long checkId, List<StockCheckItemDTO> items);

    /**
     * 录入实际库存数量
     *
     * @param checkId 盘点单ID
     * @param items   [{materialId, actualStock}]
     */
    void recordActualQty(Long checkId, List<StockCheckItemDTO> items);
}
