package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.module.inventory.model.StockCheck;
import java.util.List;

/**
 * 库存盘点服务接口
 * 提供盘点单创建、盘点完成确认等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface StockCheckService extends IService<StockCheck> {

    /**
     * 创建盘点单
     *
     * @param operator 操作人
     * @param remark   备注
     * @return 盘点单
     */
    StockCheck createCheck(String operator, String remark);

    /**
     * 完成盘点（提交盘点结果并调整库存）
     *
     * @param checkId 盘点单ID
     * @param items   盘点明细列表
     */
    void completeCheck(Long checkId, List<StockCheckItemDTO> items);
}
