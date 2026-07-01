package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.DiningTable;

public interface DiningTableService extends IService<DiningTable> {
    void changeStatus(Long tableId, String status);
    Page<DiningTable> pageWithArea(int page, int pageSize);
}
