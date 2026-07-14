package com.reggie.module.dining.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dining.model.TableArea;

/**
 * <p>
 * 桌台区域服务接口
 * </p>
 * <p>管理堂食区域信息（如大厅、包间等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface TableAreaService extends IService<TableArea> {

    /**
     * 分页查询桌台区域信息
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页区域列表
     */
    Page<TableArea> pageQuery(int page, int pageSize);

    /**
     * 根据名称查询区域
     *
     * @param name 区域名称
     * @return 区域信息
     */
    TableArea getByName(String name);
}
