package com.reggie.module.dish.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.module.dish.model.DishEvaluation;
import com.reggie.module.dish.service.DishEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 菜品评价管理控制器
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/dish-evaluation")
@Tag(name = "菜品评价", description = "菜品评价管理相关接口")
public class DishEvaluationController {

    @Autowired
    private DishEvaluationService dishEvaluationService;

    /**
     * 新增菜品评价
     *
     * @param evaluation 评价信息
     * @return 新增结果
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增菜品评价", description = "用户对已购买菜品进行评价，需登录")
    @Parameter(name = "evaluation", description = "评价信息（订单ID、菜品ID、评分、内容等）", required = true)
    public R<DishEvaluation> addEvaluation(@Valid @RequestBody DishEvaluation evaluation) {
        log.info("用户新增菜品评价：userId={}, dishId={}, starRating={}",
                BaseContext.getCurrentId(), evaluation.getDishId(), evaluation.getStarRating());

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        // 设置用户信息
        evaluation.setUserId(BaseContext.getCurrentId());

        DishEvaluation result = dishEvaluationService.addEvaluation(evaluation);
        return R.success(result);
    }

    /**
     * 获取菜品评价列表（分页，只返回已通过审核的评价）
     *
     * @param dishId    菜品ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 评价分页列表
     */
    @GetMapping("/dish/{dishId}")
    @Operation(summary = "获取菜品评价列表", description = "分页查询指定菜品的已通过审核评价，按时间倒序")
    @Parameter(name = "dishId", description = "菜品ID", required = true)
    public R<Page<DishEvaluation>> listByDishId(
            @PathVariable Long dishId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer page,
            @Parameter(description = "PageSize")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1") @Max(value = 50, message = "每页最多50条") Integer pageSize) {

        log.info("查询菜品评价列表：dishId={}, page={}, pageSize={}", dishId, page, pageSize);

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        Page<DishEvaluation> result = dishEvaluationService.pageByDishId(tenantId, dishId, page, pageSize);
        return R.success(result);
    }

    /**
     * 获取菜品评分统计数据
     *
     * @param dishId 菜品ID
     * @return 评分统计信息（avgRating, totalCount, reviewCount）
     */
    @GetMapping("/dish/{dishId}/stats")
    @Operation(summary = "获取菜品评分统计", description = "获取指定菜品的平均评分、评价总数、去重评价人数")
    @Parameter(name = "dishId", description = "菜品ID", required = true)
    public R<Map<String, Object>> getDishRatingStats(@PathVariable Long dishId) {
        log.info("查询菜品评分统计：dishId={}", dishId);

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        Map<String, Object> stats = dishEvaluationService.getDishRatingStats(tenantId, dishId);
        return R.success(stats);
    }

    /**
     * 获取我的评价列表（分页）
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @return 我的评价分页列表
     */
    @GetMapping("/user/my")
    @Operation(summary = "获取我的评价列表", description = "查询当前登录用户的历史评价，按时间倒序")
    public R<Page<DishEvaluation>> listMyEvaluations(
            @Parameter(description = "Page")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer page,
            @Parameter(description = "PageSize")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1") @Max(value = 50, message = "每页最多50条") Integer pageSize) {

        log.info("查询我的评价列表：userId={}, page={}, pageSize={}", BaseContext.getCurrentId(), page, pageSize);

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        Page<DishEvaluation> result = dishEvaluationService.pageByUserId(tenantId, BaseContext.getCurrentId(), page, pageSize);
        return R.success(result);
    }

    /**
     * 商家回复评价
     *
     * @param id           评价ID
     * @param replyContent 回复内容
     * @return 回复结果
     */
    @PutMapping("/{id}/reply")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "商家回复评价", description = "商家对已通过审核的评价进行回复，需商家/管理员权限")
    @Parameter(name = "id", description = "评价ID", required = true)
    @RequiresPermission("evaluation:reply")
    public R<String> replyEvaluation(
            @PathVariable Long id,
            @Parameter(name = "replyContent", description = "回复内容", required = true)
            @RequestBody Map<String, String> params) {

        String replyContent = params.get("replyContent");
        if (replyContent == null || replyContent.trim().isEmpty()) {
            return R.error("回复内容不能为空");
        }

        log.info("商家回复评价：evaluationId={}, replyUserId={}", id, BaseContext.getCurrentId());

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        boolean result = dishEvaluationService.replyEvaluation(id, replyContent, BaseContext.getCurrentId(), tenantId);
        if (result) {
            return R.success("回复成功");
        }
        return R.error("回复失败");
    }

    /**
     * 审核评价
     *
     * @param id     评价ID
     * @param status 审核状态（1通过 2拒绝）
     * @return 审核结果
     */
    @PutMapping("/{id}/status")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "审核评价", description = "商家/管理员审核评价，通过或拒绝，需商家/管理员权限")
    @Parameter(name = "id", description = "评价ID", required = true)
    @RequiresPermission("evaluation:audit")
    public R<String> updateStatus(
            @PathVariable Long id,
            @Parameter(name = "status", description = "审核状态（1通过 2拒绝）", required = true)
            @RequestBody Map<String, Integer> params) {

        Integer status = params.get("status");
        if (status == null || (status != 1 && status != 2)) {
            return R.error("审核状态不合法，只能为1（通过）或2（拒绝）");
        }

        log.info("审核评价：evaluationId={}, status={}, operatorId={}", id, status, BaseContext.getCurrentId());

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        boolean result = dishEvaluationService.updateStatus(id, status);
        if (result) {
            return R.success(status == 1 ? "审核通过" : "审核拒绝");
        }
        return R.error("审核失败");
    }

    /**
     * 根据订单ID获取评价
     *
     * @param orderId 订单ID
     * @return 评价列表
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID获取评价", description = "查询指定订单下所有菜品的评价")
    @Parameter(name = "orderId", description = "订单ID", required = true)
    public R<List<DishEvaluation>> listByOrderId(@PathVariable Long orderId) {
        log.info("根据订单ID查询评价：orderId={}", orderId);

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        List<DishEvaluation> result = dishEvaluationService.listByOrderId(tenantId, orderId);
        return R.success(result);
    }

    /**
     * 获取待审核评价列表（分页）
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @return 待审核评价分页列表
     */
    @GetMapping("/pending")
    @Operation(summary = "获取待审核评价列表", description = "分页查询待审核的评价，需商家/管理员权限")
    @RequiresPermission("evaluation:view")
    public R<Page<DishEvaluation>> listPending(
            @Parameter(description = "Page")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Integer page,
            @Parameter(description = "PageSize")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1") @Max(value = 50, message = "每页最多50条") Integer pageSize) {

        log.info("查询待审核评价列表：page={}, pageSize={}", page, pageSize);

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        LambdaQueryWrapper<DishEvaluation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishEvaluation::getTenantId, tenantId)
                .eq(DishEvaluation::getStatus, 0)
                .orderByDesc(DishEvaluation::getCreateTime);

        Page<DishEvaluation> pageObj = PageUtils.of(page, pageSize);
        Page<DishEvaluation> evaluations = dishEvaluationService.page(pageObj, queryWrapper);
        return R.success(evaluations);
    }

    /**
     * 管理端评价分页查询（支持菜品名称、状态、评分、回复状态筛选）
     *
     * @param dishName    菜品名称（可选，模糊查询）
     * @param status      审核状态（可选）
     * @param starRating  评分（可选）
     * @param replyStatus 回复状态（可选）：0=未回复，1=已回复
     * @param page        页码
     * @param pageSize    每页条数
     * @return 分页评价列表
     */
    @GetMapping("/page")
    @Operation(summary = "管理端评价分页查询", description = "支持按菜品名称、审核状态、评分、回复状态筛选的评价管理列表")
    public R<Page<DishEvaluation>> adminPage(
            @Parameter(name = "dishName", description = "菜品名称（模糊查询）") @RequestParam(required = false) String dishName,
            @Parameter(name = "status", description = "审核状态（0待审核 1通过 2拒绝）") @RequestParam(required = false) Integer status,
            @Parameter(name = "starRating", description = "评分（1-5）") @RequestParam(required = false) Integer starRating,
            @Parameter(name = "replyStatus", description = "回复状态（0未回复 1已回复）") @RequestParam(required = false) Integer replyStatus,
            @Parameter(name = "page", description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(name = "pageSize", description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {

        log.info("[Evaluation] 管理端评价查询：dishName={}, status={}, starRating={}, replyStatus={}, page={}, pageSize={}",
                dishName, status, starRating, replyStatus, page, PageUtils.cap(pageSize));

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        Page<DishEvaluation> result = dishEvaluationService.adminPage(
                tenantId, dishName, status, starRating, replyStatus, page, PageUtils.cap(pageSize));
        return R.success(result);
    }

    /**
     * 删除自己的评价（仅未审核且本人评价可删）
     *
     * @param params 包含 id 的请求体
     * @return 删除结果
     */
    @DeleteMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除自己的评价", description = "用户删除自己未审核的评价，需登录")
    public R<String> deleteMyEvaluation(@RequestBody Map<String, Long> params) {
        Long id = params.get("id");
        if (id == null) {
            return R.error("评价ID不能为空");
        }

        log.info("删除评价：evaluationId={}, userId={}", id, BaseContext.getCurrentId());

        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("租户信息缺失");
        }

        boolean result = dishEvaluationService.deleteMyEvaluation(id, BaseContext.getCurrentId(), tenantId);
        if (result) {
            return R.success("删除成功");
        }
        return R.error("删除失败");
    }
}




