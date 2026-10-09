package com.reggie.module.notification.provider;

import com.reggie.module.notification.model.PushMessage;
import com.reggie.module.notification.model.UserDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 模拟推送Provider，无需接入第三方SDK即可使用，用于演示、测试或无推送平台环境。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class MockPushProvider implements PushProvider {

    /**
     * 向单个设备推送消息
     *
     * @param device  用户设备信息
     * @param message 推送消息
     * @return 是否推送成功
     */
    @Override
    public boolean pushToDevice(UserDevice device, PushMessage message) {
        log.debug("[APP推送Mock] userId={}, platform={}, token={}, title={}, content={}, clickAction={}, extras={}",
                device.getUserId(),
                device.getPlatform(),
                maskToken(device.getDeviceToken()),
                message.getTitle(),
                message.getContent(),
                message.getClickAction(),
                message.getExtras());
        return true;
    }

    /**
     * 向用户的所有设备批量推送消息
     *
     * @param devices 用户设备列表
     * @param message 推送消息
     * @return 成功推送的设备数量
     */
    @Override
    public int pushToUserDevices(List<UserDevice> devices, PushMessage message) {
        if (devices == null || devices.isEmpty()) {
            log.debug("[APP推送Mock] 用户无可用设备，跳过推送");
            return 0;
        }

        int successCount = 0;
        for (UserDevice device : devices) {
            boolean ok = pushToDevice(device, message);
            if (ok) {
                successCount++;
            }
        }
        log.info("[APP推送Mock] 批量推送完成: userId={}, 设备数={}, 成功={}",
                devices.get(0).getUserId(), devices.size(), successCount);
        return successCount;
    }

    /**
     * 获取提供商名称
     *
     * @return 提供商标识
     */
    @Override
    public String getProviderName() {
        return "mock";
    }

    /**
     * 是否为模拟模式
     *
     * @return 是否为模拟模式
     */
    @Override
    public boolean isMockMode() {
        return true;
    }

    /**
     * 脱敏Token用于日志输出
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
