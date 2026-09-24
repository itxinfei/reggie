package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.PasswordUtils;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.common.annotation.RequireRider;
import com.reggie.module.delivery.mapper.RiderMapper;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 骑手端认证控制器。
 * <p>
 * 提供骑手「手机号 + 密码」登录、登出、当前信息与上下线切换。
 * 登录成功后会话写 rider / tenantId，由 {@code LoginCheckFilter} 识别为骑手身份，
 * 业务接口通过 {@link RequireRider} 限定仅骑手可访问。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Slf4j
@RestController
@RequestMapping("/api/rider")
@Tag(name = "骑手认证", description = "骑手登录/登出/会话/上下线")
public class RiderAuthController {

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    /**
     * 骑手登录（手机号 + 密码）。
     * @param request HTTP 请求
     * @param body 含 phone、password
     * @return 骑手信息（密码不返回）
     */
    @PostMapping("/login")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.IP)
    @Operation(summary = "骑手登录", description = "手机号 + 密码登录骑手端")
    public R<Rider> login(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String phone = body == null ? null : body.get("phone");
        String password = body == null ? null : body.get("password");
        if (phone == null || phone.trim().isEmpty() || password == null || password.isEmpty()) {
            return R.error("手机号和密码不能为空");
        }

        // 登录前无租户上下文，selectByPhone 内部关闭了租户过滤
        Rider rider = riderMapper.selectByPhone(phone.trim());
        // 账号不存在或未设置密码（历史档案骑手），统一返回相同提示，避免账号枚举
        if (rider == null || rider.getPassword() == null || rider.getPassword().isEmpty()) {
            return R.error("手机号或密码错误");
        }
        if (!PasswordUtils.matches(password, rider.getPassword())) {
            return R.error("手机号或密码错误");
        }

        // 建立骑手会话
        request.getSession().setAttribute("rider", rider.getId());
        request.getSession().setAttribute("tenantId", rider.getTenantId());
        // 防止 Session Fixation：登录后切换 Session ID（须在写入属性后调用）
        request.changeSessionId();
        log.info("骑手登录成功：riderId={}, tenantId={}", rider.getId(), rider.getTenantId());

        Rider fresh = deliveryTrackingService.getRiderById(rider.getId());
        return R.success(fresh != null ? fresh : rider);
    }

    /**
     * 骑手登出。
     * @param request HTTP 请求
     * @return 结果
     */
    @PostMapping("/logout")
    @Operation(summary = "骑手登出", description = "退出骑手端并销毁会话")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        BaseContext.remove();
        return R.success("退出成功");
    }

    /**
     * 获取当前登录骑手信息。
     * @return 骑手信息
     */
    @GetMapping("/me")
    @RequireRider
    @Operation(summary = "当前骑手信息", description = "获取当前登录骑手资料")
    public R<Rider> me() {
        Rider rider = deliveryTrackingService.getRiderById(BaseContext.getCurrentId());
        if (rider == null) {
            return R.error("骑手信息不存在");
        }
        return R.success(rider);
    }

    /**
     * 骑手上班（上线，可接单/抢单）。
     * @return 结果
     */
    @PostMapping("/online")
    @RequireRider
    @Operation(summary = "骑手上线", description = "切换为在线，可被派单或抢单")
    public R<String> online() {
        boolean ok = deliveryTrackingService.updateRiderStatus(BaseContext.getCurrentId(), Rider.STATUS_ONLINE);
        return ok ? R.success("已上线") : R.error("状态更新失败");
    }

    /**
     * 骑手下班（下线，不再接单）。
     * @return 结果
     */
    @PostMapping("/offline")
    @RequireRider
    @Operation(summary = "骑手下线", description = "切换为离线，不再接收新订单")
    public R<String> offline() {
        boolean ok = deliveryTrackingService.updateRiderStatus(BaseContext.getCurrentId(), Rider.STATUS_OFFLINE);
        return ok ? R.success("已下线") : R.error("状态更新失败");
    }

    /**
     * 骑手上报实时位置（骑手端每 12s 定时上报，用于顾客配送追踪页地图与距离展示）。
     * <p>骑手ID 只从会话取，不信任请求体，防止冒充他人上报。</p>
     *
     * @param body 含 longitude、latitude，可选 speed、heading
     * @return 结果
     */
    @PostMapping("/location")
    @RequireRider
    @RateLimit(maxRequestsPerSecond = 5)
    @Operation(summary = "骑手上报位置", description = "骑手端定时上报GPS坐标，供顾客端配送追踪使用")
    public R<String> reportLocation(@RequestBody Map<String, Object> body) {
        BigDecimal longitude = toBigDecimal(body == null ? null : body.get("longitude"));
        BigDecimal latitude = toBigDecimal(body == null ? null : body.get("latitude"));
        if (longitude == null || latitude == null) {
            return R.error("经纬度不能为空");
        }
        if (longitude.abs().compareTo(new BigDecimal("180")) > 0
                || latitude.abs().compareTo(new BigDecimal("90")) > 0) {
            return R.error("经纬度格式不正确");
        }
        BigDecimal speed = toBigDecimal(body == null ? null : body.get("speed"));
        BigDecimal heading = toBigDecimal(body == null ? null : body.get("heading"));
        boolean ok = deliveryTrackingService.updateRiderLocation(
                BaseContext.getCurrentId(), longitude, latitude, speed, heading);
        return ok ? R.success("位置已更新") : R.error("位置上报失败");
    }

    /**
     * 将请求体中的数字（JSON 反序列化为 Integer/Double 等）统一转为 BigDecimal；
     * null 或无法解析时返回 null。
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
