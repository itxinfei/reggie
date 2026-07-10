package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.DiningTable;

/**
 * 堂食桌台服务接口
 * 提供桌台状态管理、分页查询等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface DiningTableService extends IService<DiningTable> {

    /**
     * 更改桌台状态
     *
     * @param tableId 桌台ID
     * @param status  目标状态
     */
    void changeStatus(Long tableId, String status);

    /**
     * 分页查询桌台信息（关联区域名称）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页桌台列表
     */
    Page<DiningTable> pageWithArea(int page, int pageSize);
}
