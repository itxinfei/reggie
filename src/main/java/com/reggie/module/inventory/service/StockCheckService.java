package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.StockCheck;
import java.util.List;
import java.util.Map;

public interface StockCheckService extends IService<StockCheck> {
    StockCheck createCheck(String operator, String remark);
    void completeCheck(Long checkId, List<Map<String, Object>> items);
}
