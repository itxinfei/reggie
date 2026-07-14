package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.Supplier;

import java.util.List;

/**
 * <p>
 * 供应商管理服务接口
 * </p>
 * <p>管理供应商信息（名称、联系方式、合作状态等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface SupplierService extends IService<Supplier> {

    /**
     * 分页查询供应商
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param name     供应商名称（可选，模糊查询）
     * @return 分页供应商列表
     */
    Page<Supplier> pageQuery(int page, int pageSize, String name);

    /**
     * 查询所有启用状态的供应商
     *
     * @return 启用状态的供应商列表
     */
    List<Supplier> listEnabled();
}
