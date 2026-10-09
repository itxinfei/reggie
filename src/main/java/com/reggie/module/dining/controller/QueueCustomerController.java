package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireUser;
import com.reggie.module.dining.dto.CustomerTakeQueueDTO;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.QueueService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * C 端顾客排队取号控制器。
 * 与员工端 {@link QueueController}（/api/dining/queue，@RequireEmployee）分离：
 * 本控制器只暴露取号 / 查本人排队 / 本人取消，归属校验在 Service 兜底。
 *
 * @author reggie
 * @since 2026-09-29
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/dining/queue/customer")
@Tag(name = "顾客排队取号")
@RequireUser
public class QueueCustomerController {

    @Autowired
    private QueueService queueService;

    @Autowired
    private UserService userService;

    /**
     * 顾客取号（手机号自动取登录用户的，无需填写）
     */
    @PostMapping("/take")
    @Operation(summary = "顾客取号")
    public R<QueueRecord> take(@Valid @RequestBody CustomerTakeQueueDTO dto) {
        Long userId = BaseContext.getCurrentId();
        User user = userService.getById(userId);
        String phone = user != null ? user.getPhone() : null;
        log.info("[顾客取号] userId={}, seatCount={}", userId, dto.getSeatCount());
        return R.success(queueService.takeNumber(dto.getSeatCount(), phone, userId));
    }

    /**
     * 查询本人当前排队（最新一条 WAITING/CALLED）及前面等待桌数；无记录 data 为 null
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的排队")
    public R<Map<String, Object>> my() {
        Long userId = BaseContext.getCurrentId();
        QueueRecord record = queueService.lambdaQuery()
                .eq(QueueRecord::getUserId, userId)
                .in(QueueRecord::getStatus,
                        com.reggie.enums.QueueRecordStatus.WAITING.getValue(),
                        com.reggie.enums.QueueRecordStatus.CALLED.getValue())
                .orderByDesc(QueueRecord::getCreatedTime)
                .last("LIMIT 1")
                .one();
        Map<String, Object> data = null;
        if (record != null) {
            data = new HashMap<>();
            data.put("record", record);
            data.put("waitingAhead", queueService.countWaitingAhead(record.getId()));
        }
        return R.success(data);
    }

    /**
     * 顾客取消自己的排队
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消我的排队")
    public R<String> cancel(@PathVariable Long id) {
        queueService.cancelMyQueue(id, BaseContext.getCurrentId());
        return R.success("取消排队成功");
    }
}
