package com.reggie.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.dish.model.DishEvaluation;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.dish.mapper.DishEvaluationMapper;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.dish.service.DishEvaluationService;
import com.reggie.module.order.service.OrderService;
import org.apache.commons.text.StringEscapeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

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
     * @return 评价信息，不存在返回null
     */
    @Override
    public DishEvaluation getById(Long id) {
        return super.getById(id);
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

        Page<DishEvaluation> pageObj = PageUtils.of(page, pageSize);
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

        Page<DishEvaluation> pageObj = PageUtils.of(page, pageSize);
        pageObj.setTotal(total);
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

        Page<DishEvaluation> pageObj = PageUtils.of(page, pageSize);
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

        Long userId = evaluation.getUserId();
        Long orderId = evaluation.getOrderId();
        Long dishId = evaluation.getDishId();

        // 校验用户ID
        if (userId == null) {
            throw new CustomException("用户信息缺失");
        }

        // 校验订单ID
        if (orderId == null) {
            throw new CustomException("订单ID不能为空");
        }

        // 校验菜品ID
        if (dishId == null) {
            throw new CustomException("菜品ID不能为空");
        }

        // 查询订单信息，校验订单存在性和归属
        Orders order = orderService.getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }

        // 校验订单归属（当前用户必须是订单的下单用户）
        if (!userId.equals(order.getUserId())) {
            throw new CustomException("无权评价该订单");
        }

        // 校验订单状态（必须是已完成状态才能评价）
        if (order.getStatus() == null || order.getStatus() != Orders.STATUS_COMPLETED) {
            throw new CustomException("订单未完成，无法评价");
        }

        // 校验菜品是否属于该订单
        LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, orderId)
                .eq(OrderDetail::getDishId, dishId)
                .eq(OrderDetail::getIsDeleted, 0);
        OrderDetail orderDetail = orderDetailMapper.selectOne(detailWrapper);
        if (orderDetail == null) {
            throw new CustomException("该菜品不属于此订单");
        }

        // 校验是否已评价过该菜品（防止重复评价）
        LambdaQueryWrapper<DishEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(DishEvaluation::getOrderId, orderId)
                .eq(DishEvaluation::getDishId, dishId);
        if (this.count(evalWrapper) > 0) {
            throw new CustomException("该菜品已评价过，不能重复评价");
        }

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

        // XSS防护：对评价内容和菜品名称进行HTML转义
        if (evaluation.getContent() != null) {
            evaluation.setContent(StringEscapeUtils.escapeHtml4(evaluation.getContent()));
        }
        if (evaluation.getDishName() != null) {
            evaluation.setDishName(StringEscapeUtils.escapeHtml4(evaluation.getDishName()));
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
     * @param tenantId     租户ID
     * @return 是否回复成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyEvaluation(Long id, String replyContent, Long replyUserId, Long tenantId) {
        log.info("商家回复评价：evaluationId={}, replyUserId={}, tenantId={}", id, replyUserId, tenantId);

        DishEvaluation evaluation = this.getById(id);
        if (evaluation == null) {
            throw new CustomException("评价不存在");
        }

        // 多租户校验
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

        // XSS防护：对商家回复内容进行HTML转义
        if (replyContent != null) {
            replyContent = StringEscapeUtils.escapeHtml4(replyContent);
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
     * 删除自己的评价（仅限评价人本人删除未审核的评价）
     *
     * @param id       评价ID
     * @param userId   当前用户ID
     * @param tenantId 租户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMyEvaluation(Long id, Long userId, Long tenantId) {
        DishEvaluation evaluation = this.getById(id);
        if (evaluation == null) {
            throw new CustomException("评价不存在");
        }

        // 多租户校验
        if (tenantId == null || !tenantId.equals(evaluation.getTenantId())) {
            throw new CustomException("无权操作该评价");
        }

        // 仅评价人本人可删除
        if (!evaluation.getUserId().equals(userId)) {
            throw new CustomException("只能删除自己的评价");
        }

        // 仅未审核的评价可删除
        if (evaluation.getStatus() != null && evaluation.getStatus() != 0) {
            throw new CustomException("已审核的评价无法删除");
        }

        boolean result = this.removeById(id);
        if (result) {
            log.info("评价删除成功：evaluationId={}, userId={}", id, userId);
        }
        return result;
    }
}



