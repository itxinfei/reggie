package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.service.PrintTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 打印任务服务实现（管理端任务查询 / 统计）
 *
 * <p>统计走自定义 {@code @InterceptorIgnore(tenantLine = "true")} 的 countBy，
 * 支持门店员工（tenantId 非空）与总部超管（tenantId 为空=跨门店全部）两种视角。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Service
public class PrintTaskServiceImpl extends ServiceImpl<PrintTaskMapper, PrintTask> implements PrintTaskService {

    @Override
    public Map<String, Object> statTasks(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.toLocalDate().atStartOfDay();
        Map<String, Object> result = new HashMap<>();
        result.put("totalTasks", getBaseMapper().countBy(tenantId, false, today, null));
        result.put("todayTotal", getBaseMapper().countBy(tenantId, true, today, null));
        result.put("todaySuccess", getBaseMapper().countBy(tenantId, true, today,
                Arrays.asList(PrintTask.STATUS_SUCCESS)));
        result.put("todayFailed", getBaseMapper().countBy(tenantId, true, today,
                Arrays.asList(PrintTask.STATUS_FAILED)));
        result.put("pending", getBaseMapper().countBy(tenantId, false, today,
                Arrays.asList(PrintTask.STATUS_PENDING, PrintTask.STATUS_PULLED)));
        return result;
    }

    @Override
    public IPage<PrintTask> pageQuery(int page, int pageSize, Long tenantId, Long orderId, String taskType,
                                      String status, LocalDateTime beginTime, LocalDateTime endTime) {
        return getBaseMapper().listPage(PageUtils.of(page, pageSize), tenantId, orderId, taskType,
                status, beginTime, endTime);
    }
}
