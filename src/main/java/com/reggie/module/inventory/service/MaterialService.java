package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.Material;
import java.util.List;

/**
 * 原料管理服务接口
 * 提供原料信息维护、分页查询、库存预警等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface MaterialService extends IService<Material> {

    /**
     * 分页查询原料信息（关联分类名称）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 分页原料列表
     */
    Page<Material> pageWithCategory(int page, int pageSize);

    /**
     * 查询库存预警的原料列表（库存低于预警阈值）
     *
     * @return 需要补货的原料列表
     */
    List<Material> checkWarning();
}
