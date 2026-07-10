package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.entity.DishEvaluation;
import com.reggie.mapper.DishEvaluationMapper;
import com.reggie.service.DishEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 菜品评价服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Slf4j
public class DishEvaluationServiceImpl extends ServiceImpl<DishEvaluationMapper, DishEvaluation>
        implements DishEvaluationService {

    /**
     * 最小评分
     */
    private static final int MIN_STAR_RATING = 1;

    /**
     * 最大评分
     */
    private static final int MAX_STAR_RATING = 5;

    /**
     * 评价内容最大长度
     */
    private static final int MAX_CONTENT_LENGTH = 500;

    /**
     * 根据ID查询评价
     *
     * @param id 评价ID
     * @return 评价信息
     */
    @Override
    public DishEvaluation getById(Long id) {
        return this.getById(id);
    }

    /**
     * 根据菜品ID查询评价列表
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @param status   审核状态（null表示查询已通过的）
     * @return 评价列表
     */
    @Override
    public List<DishEvaluation> listByDishId(Long tenantId, Long dishId, Integer status) {
        LambdaQueryWrapper<DishEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .eq(DishEvaluation::getDishId, dishId)
                .eq(status != null, DishEvaluation::getStatus, status)
                .orderByDesc(DishEvaluation::getCreateTime);
        // 当status为null时，默认查询已通过(status=1)的评价
        if (status == null) {
            queryWrapper.eq(DishEvaluation::getStatus, 1);
        }
        return this.list(queryWrapper);
    }

    /**
     * 根据菜品ID分页查询评价列表
     *
     * @param tenantId  租户ID
     * @param dishId    菜品ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 分页评价列表
     */
    @Override
    public Page<DishEvaluation> pageByDishId(Long tenantId, Long dishId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<DishEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .eq(DishEvaluation::getDishId, dishId)
                .eq(DishEvaluation::getStatus, 1)
                .orderByDesc(DishEvaluation::getCreateTime);

        Page<DishEvaluation> pageObj = new Page<>(page, pageSize);
        return this.page(pageObj, queryWrapper);
    }

    /**
     * 根据用户ID分页查询评价列表
     *
     * @param tenantId  租户ID
     * @param userId    用户ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 分页评价列表
     */
    @Override
    public Page<DishEvaluation> pageByUserId(Long tenantId, Long userId, Integer page, Integer pageSize) {
        // 使用自定义Mapper方法进行分页（LIMIT offset, pageSize）
        int offset = (page - 1) * pageSize;
        List<DishEvaluation> records = baseMapper.listByUserId(tenantId, userId, offset, pageSize);

        // 统计总数
        LambdaQueryWrapper<DishEvaluation> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .eq(DishEvaluation::getUserId, userId);
        long total = this.count(countWrapper);

        Page<DishEvaluation> pageObj = new Page<>(page, pageSize, total);
        pageObj.setRecords(records);
        return pageObj;
    }

    @Override
    public Page<DishEvaluation> adminPage(Long tenantId, String dishName, Integer status,
                                           Integer starRating, Integer page, Integer pageSize) {
        LambdaQueryWrapper<DishEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .like(dishName != null && !dishName.isEmpty(), DishEvaluation::getDishName, dishName)
                .eq(status != null, DishEvaluation::getStatus, status)
                .eq(starRating != null, DishEvaluation::getStarRating, starRating)
                .orderByDesc(DishEvaluation::getCreateTime);

        Page<DishEvaluation> pageObj = new Page<>(page, pageSize);
        return this.page(pageObj, queryWrapper);
    }

    /**
     * 新增菜品评价
     *
     * @param evaluation 评价信息
     * @return 新增的评价
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DishEvaluation addEvaluation(DishEvaluation evaluation) {
        log.info("新增菜品评价：userId={}, dishId={}, starRating={}",
                evaluation.getUserId(), evaluation.getDishId(), evaluation.getStarRating());

        // 校验评分范围
        Integer starRating = evaluation.getStarRating();
        if (starRating == null || starRating < MIN_STAR_RATING || starRating > MAX_STAR_RATING) {
            throw new CustomException("评分必须在" + MIN_STAR_RATING + "-" + MAX_STAR_RATING + "分之间");
        }

        // 校验评价内容长度
        String content = evaluation.getContent();
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException("评价内容不能超过" + MAX_CONTENT_LENGTH + "个字符");
        }

        // 设置默认审核状态为待审核
        if (evaluation.getStatus() == null) {
            evaluation.setStatus(0);
        }

        // 设置租户ID（从当前上下文获取）
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户信息缺失");
        }
        evaluation.setTenantId(tenantId);

        // 设置创建人和修改人
        Long currentUserId = BaseContext.getCurrentId();
        evaluation.setCreateUser(currentUserId);
        evaluation.setUpdateUser(currentUserId);

        // 保存评价
        this.save(evaluation);
        log.info("菜品评价新增成功：evaluationId={}", evaluation.getId());
        return evaluation;
    }

    /**
     * 商家回复评价
     *
     * @param id           评价ID
     * @param replyContent 回复内容
     * @param replyUserId  回复人ID
     * @return 是否回复成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyEvaluation(Long id, String replyContent, Long replyUserId) {
        log.info("商家回复评价：evaluationId={}, replyUserId={}", id, replyUserId);

        DishEvaluation evaluation = this.getById(id);
        if (evaluation == null) {
            throw new CustomException("评价不存在");
        }

        // 多租户校验
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null || !tenantId.equals(evaluation.getTenantId())) {
            throw new CustomException("无权操作该评价");
        }

        // 评价必须已通过审核才能回复
        if (evaluation.getStatus() != null && evaluation.getStatus() != 1) {
            throw new CustomException("该评价未通过审核，无法回复");
        }

        // 检查是否已回复
        if (evaluation.getReplyContent() != null && !evaluation.getReplyContent().isEmpty()) {
            throw new CustomException("该评价已回复，不能重复回复");
        }

        // 设置回复信息
        evaluation.setReplyContent(replyContent);
        evaluation.setReplyTime(LocalDateTime.now());
        evaluation.setUpdateUser(replyUserId);

        boolean result = this.updateById(evaluation);
        if (result) {
            log.info("评价回复成功：evaluationId={}", id);
        } else {
            log.warn("评价回复失败：evaluationId={}", id);
        }
        return result;
    }

    /**
     * 更新评价审核状态
     *
     * @param id     评价ID
     * @param status 审核状态（1通过 2拒绝）
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        log.info("更新评价审核状态：evaluationId={}, status={}", id, status);

        DishEvaluation evaluation = this.getById(id);
        if (evaluation == null) {
            throw new CustomException("评价不存在");
        }

        // 多租户校验
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null || !tenantId.equals(evaluation.getTenantId())) {
            throw new CustomException("无权操作该评价");
        }

        // 状态校验
        if (status != null && status != 1 && status != 2) {
            throw new CustomException("审核状态不合法，只能为1（通过）或2（拒绝）");
        }

        evaluation.setStatus(status);
        Long currentUserId = BaseContext.getCurrentId();
        evaluation.setUpdateUser(currentUserId);

        boolean result = this.updateById(evaluation);
        if (result) {
            log.info("评价审核状态更新成功：evaluationId={}, status={}", id, status);
        } else {
            log.warn("评价审核状态更新失败：evaluationId={}", id);
        }
        return result;
    }

    /**
     * 获取菜品评分统计数据
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分统计 Map（avgRating, totalCount, reviewCount）
     */
    @Override
    public Map<String, Object> getDishRatingStats(Long tenantId, Long dishId) {
        return baseMapper.getDishRatingStats(tenantId, dishId);
    }

    /**
     * 获取菜品评分分布（各分数段评价数量）
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 评分分布列表
     */
    @Override
    public List<Map<String, Object>> getDishRatingDistribution(Long tenantId, Long dishId) {
        return baseMapper.getDishRatingDistribution(tenantId, dishId);
    }

    /**
     * 获取菜品平均评分
     *
     * @param tenantId 租户ID
     * @param dishId   菜品ID
     * @return 平均评分
     */
    @Override
    public Double getAverageRating(Long tenantId, Long dishId) {
        Double avgRating = baseMapper.getAverageRating(tenantId, dishId);
        return avgRating != null ? avgRating : 0.0;
    }

    /**
     * 根据订单ID查询评价列表
     *
     * @param tenantId 租户ID
     * @param orderId  订单ID
     * @return 评价列表
     */
    @Override
    public List<DishEvaluation> listByOrderId(Long tenantId, Long orderId) {
        LambdaQueryWrapper<DishEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .eq(DishEvaluation::getOrderId, orderId)
                .orderByDesc(DishEvaluation::getCreateTime);
        return this.list(queryWrapper);
    }

    /**
     * 统计指定状态的评价数量
     *
     * @param tenantId 租户ID
     * @param status   审核状态
     * @return 评价数量
     */
    @Override
    public int countByStatus(Long tenantId, Integer status) {
        return baseMapper.countByStatus(tenantId, status);
    }
}
