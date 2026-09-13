package com.reggie.module.billing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.billing.model.BillingPlan;

import java.util.List;

/**
 * SaaS套餐方案服务
 *
 * @author reggie
 * @since 2026-09-12
 */
public interface BillingPlanService extends IService<BillingPlan> {

    /**
     * 分页查询套餐（平台管理用，含下架套餐）。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param keyword  名称/编码关键字（可选）
     * @param status   状态（可选）
     * @return 分页结果
     */
    Page<BillingPlan> pageQuery(int page, int pageSize, String keyword, Integer status);

    /**
     * 查询所有上架套餐，按 sortOrder 升序，供租户选购。
     *
     * @return 上架套餐列表
     */
    List<BillingPlan> listOnShelf();

    /**
     * 新增套餐，校验编码唯一。
     *
     * @param plan 套餐
     * @return 保存后的套餐
     */
    BillingPlan createPlan(BillingPlan plan);

    /**
     * 更新套餐。
     *
     * @param plan 套餐
     */
    void updatePlan(BillingPlan plan);

    /**
     * 上架/下架。
     *
     * @param id     套餐ID
     * @param status 目标状态：0=下架，1=上架
     */
    void toggleStatus(Long id, Integer status);
}
