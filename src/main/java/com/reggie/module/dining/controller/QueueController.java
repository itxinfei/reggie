package com.reggie.module.dining.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.CallNextDTO;
import com.reggie.dto.TakeNumberDTO;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 排队管理控制器
 * 提供顾客取号、叫号、取消排队等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/dining/queue")
@Tag(name = "排队管理")
public class QueueController {

    @Autowired
    private QueueService queueService;

    /**
     * 分页查询排队记录
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 排队状态（可选）
     * @param phone 手机号（可选，模糊搜索）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询排队记录列表，支持按状态、手机号筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "status", description = "状态（可选）：WAITING-等待中, CALLED-已叫号, SEATED-已入座, CANCELLED-已取消")
    @Parameter(name = "phone", description = "手机号（可选，模糊搜索）")
    public R<Page<QueueRecord>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String phone) {
        Page<QueueRecord> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(status != null && !status.isEmpty(), QueueRecord::getStatus, status);
        qw.like(phone != null && !phone.isEmpty(), QueueRecord::getPhone, phone);
        qw.orderByAsc(QueueRecord::getCreatedTime);
        queueService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 顾客取号排队
     * @param dto 取号请求
     * @return 排队记录
     */
    @PostMapping("/take")
    @Operation(summary = "取号", description = "顾客取号排队，支持指定座位数和手机号")
    public R<QueueRecord> takeNumber(@Valid @RequestBody TakeNumberDTO dto) {
        log.info("取号: seatCount={}, phone={}", dto.getSeatCount(),
            dto.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        QueueRecord record = queueService.takeNumber(dto.getSeatCount(), dto.getPhone());
        return R.success(record);
    }

    /**
     * 呼叫下一位顾客
     * @param dto 叫号请求（可选）
     * @return 排队记录
     */
    @PutMapping("/call")
    @Operation(summary = "叫号", description = "呼叫下一位顾客，支持按座位数筛选")
    public R<QueueRecord> callNext(@Validated(org.springframework.validation.Validator.class) @RequestBody(required = false) CallNextDTO dto) {
        Integer seatCount = dto != null ? dto.getSeatCount() : null;
        log.info("叫号: seatCount={}", seatCount);
        QueueRecord record = queueService.callNext(seatCount);
        return record != null ? R.success(record) : R.error("没有等待中的顾客");
    }

    /**
     * 取消排队
     * @param id 排队记录ID
     * @return 操作结果
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消排队", description = "取消指定排队记录")
    @Parameter(name = "id", description = "排队记录ID", required = true)
    public R<String> cancel(@PathVariable Long id) {
        log.info("取消排队: {}", id);
        queueService.cancelQueue(id);
        return R.success("取消排队成功");
    }
}

