package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.DiningTable;

/**
 * <p>
 * 堂食桌台服务接口
 * </p>
 * <p>提供桌台状态管理、分页查询等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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
