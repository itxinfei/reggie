package com.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.DishEvaluation;

import java.util.List;
import java.util.Map;

/**
 * 菜品评价服务接口，提供评价的增删改查、评分统计等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface DishEvaluationService extends IService<DishEvaluation> {

    /**
     * 根据ID查询评价
     *
     * @param id 评价ID
     * @return 评价信息
     */
    DishEvaluation getById(Long id);

    /**
     * 根据菜品ID查询评价列表
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @param status   审核状态（null表示查询已通过的）
     * @return 评价列表
     */
    List<DishEvaluation> listByDishId(Long tenantId, Long dishId, Integer status);

    /**
     * 根据菜品ID分页查询评价列表
     *
     * @param tenantId  租户ID
     * @param dishId    菜品ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 分页评价列表
     */
    Page<DishEvaluation> pageByDishId(Long tenantId, Long dishId, Integer page, Integer pageSize);

    /**
     * 根据用户ID分页查询评价列表
     *
     * @param tenantId  租户ID
     * @param userId    用户ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 分页评价列表
     */
    Page<DishEvaluation> pageByUserId(Long tenantId, Long userId, Integer page, Integer pageSize);

    /**
     * 新增菜品评价
     *
     * @param evaluation 评价信息
     * @return 新增的评价
     */
    DishEvaluation addEvaluation(DishEvaluation evaluation);

    /**
     * 商家回复评价
     *
     * @param id           评价ID
     * @param replyContent 回复内容
     * @param replyUserId  回复人ID
     * @return 是否回复成功
     */
    boolean replyEvaluation(Long id, String replyContent, Long replyUserId);

    /**
     * 更新评价审核状态
     *
     * @param id     评价ID
     * @param status 审核状态（1通过 2拒绝）
     * @return 是否更新成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 获取菜品评分统计数据
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分统计 Map（avgRating, totalCount, reviewCount）
     */
    Map<String, Object> getDishRatingStats(Long tenantId, Long dishId);

    /**
     * 获取菜品评分分布（各分数段评价数量）
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分分布列表
     */
    List<Map<String, Object>> getDishRatingDistribution(Long tenantId, Long dishId);

    /**
     * 获取菜品平均评分
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 平均评分
     */
    Double getAverageRating(Long tenantId, Long dishId);

    /**
     * 根据订单ID查询评价列表
     *
     * @param tenantId 租户ID
     * @param orderId  订单ID
     * @return 评价列表
     */
    List<DishEvaluation> listByOrderId(Long tenantId, Long orderId);

    /**
     * 统计指定状态的评价数量
     *
     * @param tenantId 租户ID
     * @param status   审核状态
     * @return 评价数量
     */
    int countByStatus(Long tenantId, Integer status);
}
