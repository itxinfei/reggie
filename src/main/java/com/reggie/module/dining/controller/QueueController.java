package com.reggie.module.dining.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dining/queue")
@Tag(name = "排队管理")
public class QueueController {

    @Autowired
    private QueueService queueService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<QueueRecord>> page(int page, int pageSize) {
        Page<QueueRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<QueueRecord> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(QueueRecord::getCreatedTime);
        queueService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping("/take")
    @Operation(summary = "取号")
    public R<QueueRecord> takeNumber(@RequestBody Map<String, Object> params) {
        Integer seatCount = Integer.valueOf(params.get("seatCount").toString());
        String phone = (String) params.get("phone");
        log.info("取号: seatCount={}, phone={}", seatCount, phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
        QueueRecord record = queueService.takeNumber(seatCount, phone);
        return R.success(record);
    }

    @PutMapping("/call")
    @Operation(summary = "叫号")
    public R<QueueRecord> callNext(@RequestBody(required = false) Map<String, Object> params) {
        Integer seatCount = params != null && params.get("seatCount") != null
                ? Integer.valueOf(params.get("seatCount").toString()) : null;
        log.info("叫号: seatCount={}", seatCount);
        QueueRecord record = queueService.callNext(seatCount);
        return record != null ? R.success(record) : R.error("没有等待中的顾客");
    }

    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消排队")
    public R<String> cancel(@PathVariable Long id) {
        log.info("取消排队: {}", id);
        queueService.cancelQueue(id);
        return R.success("取消排队成功");
    }
}
