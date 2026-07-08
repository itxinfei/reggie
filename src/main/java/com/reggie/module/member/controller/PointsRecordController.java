package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.PointsRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/member/points")
@Tag(name = "积分记录")
public class PointsRecordController {

    @Autowired
    private PointsRecordService pointsRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询会员积分记录，支持按会员ID筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "memberId", description = "会员ID（可选）")
    public R<Page<PointsRecord>> page(int page, int pageSize, Long memberId) {
        Page<PointsRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<PointsRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(memberId != null, PointsRecord::getMemberId, memberId);
        qw.orderByDesc(PointsRecord::getCreatedTime);
        pointsRecordService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}

