package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.dto.BatchRestockDTO;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.vo.WarningMaterialVO;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 原料管理服务接口
 * </p>
 * <p>提供原料信息维护、分页查询、库存预警等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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

    /**
     * 预警食材分页查询，带严重度分级和阈值比例
     *
     * @param page       页码
     * @param pageSize   每页数量
     * @param categoryId 分类ID（可选）
     * @param severity   严重度（可选）：CRITICAL/WARNING/LOW
     * @return 预警VO分页
     */
    Page<WarningMaterialVO> warningPage(int page, int pageSize, Long categoryId, String severity);

    /**
     * 预警聚合统计
     * 返回按严重度分类的预警数量 + 按分类聚合的预警数量
     *
     * @return 统计结果
     */
    Map<String, Object> warningStats();

    /**
     * 补货建议：基于历史 N 天出库量计算日均消耗，给出建议采购量
     * 建议采购量 = 日均消耗 × 补货周期天数 - 当前库存 + 最低库存
     *
     * @param days 统计天数（默认30天）
     * @return 补货建议列表
     */
    List<Map<String, Object>> replenishSuggest(int days);

    /**
     * 批量补货：创建采购单并自动入库
     *
     * @param dto 批量补货请求
     * @return 生成的采购单ID
     */
    Long batchRestock(BatchRestockDTO dto);
}