package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireRider;
import com.reggie.module.delivery.dto.RiderIncomeRecordVO;
import com.reggie.module.delivery.dto.RiderIncomeSummaryVO;
import com.reggie.module.delivery.service.RiderIncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 骑手端收入控制器。
 * <p>提供「我的收入」汇总与明细接口；身份取自骑手会话，不信任请求体传参。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@RestController
@RequestMapping("/api/rider/income")
@Tag(name = "骑手收入", description = "骑手端我的收入汇总与明细")
public class RiderIncomeController {

    @Autowired
    private RiderIncomeService riderIncomeService;

    /**
     * 骑手收入汇总（今日 / 本周 / 本月）。
     * @return 三个窗口的汇总指标
     */
    @GetMapping("/summary")
    @RequireRider
    @Operation(summary = "骑手收入汇总", description = "返回今日/本周/本月的收入、单量、里程、准时率、平均时长")
    public R<Map<String, RiderIncomeSummaryVO>> summary() {
        return R.success(riderIncomeService.getRiderIncomeSummary(BaseContext.getCurrentId()));
    }

    /**
     * 骑手单笔收入明细（分页）。
     * @param range 时间窗口：today / week / month
     * @param page  页码
     * @param size  每页条数
     * @return 分页明细
     */
    @GetMapping("/records")
    @RequireRider
    @Operation(summary = "骑手收入明细", description = "按时间窗口分页返回单笔配送费明细")
    public R<Page<RiderIncomeRecordVO>> records(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return R.success(riderIncomeService.getRiderIncomeRecords(BaseContext.getCurrentId(), range, page, size));
    }
}
