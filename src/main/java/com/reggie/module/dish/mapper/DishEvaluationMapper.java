package com.reggie.module.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dish.model.DishEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 菜品评价 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DishEvaluationMapper extends BaseMapper<DishEvaluation> {

    /**
     * 根据菜品ID查询评价列表（只返回已通过的评价）
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @param status   审核状态（为null时查询已通过）
     * @return 评价列表
     */
    List<DishEvaluation> listByDishId(@Param("tenantId") Long tenantId,
                                       @Param("dishId") Long dishId,
                                       @Param("status") Integer status);

    /**
     * 获取菜品评分统计数据
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分统计 Map（avgRating, totalCount, reviewCount）
     */
    Map<String, Object> getDishRatingStats(@Param("tenantId") Long tenantId,
                                            @Param("dishId") Long dishId);

    /**
     * 获取菜品评分分布（各分数段评价数量）
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分分布列表
     */
    List<Map<String, Object>> getDishRatingDistribution(@Param("tenantId") Long tenantId,
                                                         @Param("dishId") Long dishId);

    /**
     * 根据用户ID分页查询评价列表
     *
     * @param tenantId  租户ID
     * @param userId    用户ID
     * @param offset    偏移量
     * @param pageSize  每页条数
     * @return 评价列表
     */
    List<DishEvaluation> listByUserId(@Param("tenantId") Long tenantId,
                                       @Param("userId") Long userId,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /**
     * 获取菜品平均评分
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 平均评分
     */
    Double getAverageRating(@Param("tenantId") Long tenantId,
                            @Param("dishId") Long dishId);

    /**
     * 统计指定状态的评价数量
     *
     * @param tenantId 租户ID
     * @param status   审核状态
     * @return 评价数量
     */
    int countByStatus(@Param("tenantId") Long tenantId,
                      @Param("status") Integer status);
}

