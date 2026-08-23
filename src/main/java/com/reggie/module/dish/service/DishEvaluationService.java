package com.reggie.module.dish.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.dish.model.DishEvaluation;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 菜品评价服务接口，提供评价的增删改查、评分统计等功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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
     * 管理端评价分页查询（支持菜品名称、审核状态、评分筛选）
     *
     * @param tenantId   租户ID
     * @param dishName   菜品名称（模糊查询，可选）
     * @param status     审核状态（可选）
     * @param starRating 评分（可选）
     * @param page       页码
     * @param pageSize   每页条数
     * @return 分页评价列表
     */
    Page<DishEvaluation> adminPage(Long tenantId, String dishName, Integer status,
                                    Integer starRating, Integer page, Integer pageSize);

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
     * @param tenantId     租户ID
     * @return 是否回复成功
     */
    boolean replyEvaluation(Long id, String replyContent, Long replyUserId, Long tenantId);

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
     * 根据订单ID查询评价列表
     *
     * @param tenantId 租户ID
     * @param orderId  订单ID
     * @return 评价列表
     */
    List<DishEvaluation> listByOrderId(Long tenantId, Long orderId);

    /**
     * 删除自己的评价（仅限评价人本人删除未审核的评价）
     *
     * @param id       评价ID
     * @param userId   当前用户ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    boolean deleteMyEvaluation(Long id, Long userId, Long tenantId);
}

