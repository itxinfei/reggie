package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.module.inventory.model.StockCheck;
import java.util.List;

public interface StockCheckService extends IService<StockCheck> {
    StockCheck createCheck(String operator, String remark);
    // 修改点：改用强类型DTO替代Map，消除类型不匹配编译错误
    void completeCheck(Long checkId, List<StockCheckItemDTO> items);
}
