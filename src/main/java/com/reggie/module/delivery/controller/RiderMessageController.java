package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequireRider;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.model.RiderMessage;
import com.reggie.module.delivery.service.RiderMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

/**
 * 骑手消息中心控制器：骑手收件箱、未读数、标记已读；管理端发布公告。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@RestController
@RequestMapping("/api/rider-message")
@Tag(name = "骑手消息中心", description = "骑手收件箱、未读数、已读标记与后台公告")
public class RiderMessageController {

    @Autowired
    private RiderMessageService riderMessageService;

    /**
     * 骑手收件箱。
     */
    @GetMapping("/mine")
    @RequireRider
    @Operation(summary = "我的消息", description = "分页查询当前登录骑手收到的消息（按时间倒序）")
    public R<Page<RiderMessage>> mine(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Integer size) {
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(riderMessageService.pageMine(riderId, tenantId, page, PageUtils.cap(size)));
    }

    /**
     * 未读消息数（工作台角标）。
     */
    @GetMapping("/unread-count")
    @RequireRider
    @Operation(summary = "未读消息数", description = "当前登录骑手未读消息条数")
    public R<Map<String, Object>> unreadCount() {
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = new HashMap<>(2);
        data.put("count", riderMessageService.unreadCount(riderId, tenantId));
        return R.success(data);
    }

    /**
     * 标记单条已读。
     */
    @PutMapping("/{id}/read")
    @RequireRider
    @Operation(summary = "标记已读", description = "将指定消息置为已读（校验归属，重复调用幂等）")
    public R<RiderMessage> markRead(@Parameter(description = "消息ID", required = true) @PathVariable Long id) {
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        return R.success(riderMessageService.markRead(id, riderId, tenantId));
    }

    /**
     * 全部标记已读。
     */
    @PutMapping("/read-all")
    @RequireRider
    @Operation(summary = "全部已读", description = "将当前骑手所有未读消息置为已读，返回受影响条数")
    public R<Map<String, Object>> markAllRead() {
        Long riderId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = new HashMap<>(2);
        data.put("count", riderMessageService.markAllRead(riderId, tenantId));
        return R.success(data);
    }

    /**
     * 管理端发布公告（广播给租户下所有骑手）。
     */
    @PostMapping("/broadcast")
    @RequireEmployee
    @Operation(summary = "发布骑手公告", description = "向当前门店全部骑手广播一条系统公告")
    public R<Map<String, Object>> broadcast(@RequestBody Map<String, String> body) {
        if (body == null || !org.springframework.util.StringUtils.hasText(body.get("title"))) {
            return R.error("公告标题不能为空");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> data = new HashMap<>(2);
        data.put("count", riderMessageService.broadcast(tenantId, body.get("title"), body.get("content")));
        return R.success(data);
    }
}
