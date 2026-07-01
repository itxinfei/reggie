package com.reggie.module.member.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.RechargeRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member/recharge")
@Tag(name = "充值记录")
public class RechargeRecordController {

    @Autowired
    private RechargeRecordService rechargeRecordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<RechargeRecord>> page(int page, int pageSize, Long memberId) {
        Page<RechargeRecord> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<RechargeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(memberId != null, RechargeRecord::getMemberId, memberId);
        qw.orderByDesc(RechargeRecord::getCreatedTime);
        rechargeRecordService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}
