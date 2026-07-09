package com.reggie.module.notification.provider;

import com.reggie.module.notification.model.PushMessage;
import com.reggie.module.notification.model.UserDevice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模拟推送Provider（无需接入第三方SDK即可使用）
 * 用于演示、测试或无推送平台环境
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class MockPushProvider implements PushProvider {

    @Override
    public boolean pushToDevice(UserDevice device, PushMessage message) {
        log.info("[APP推送Mock] userId={}, platform={}, token={}, title={}, content={}, clickAction={}, extras={}",
                device.getUserId(),
                device.getPlatform(),
                maskToken(device.getDeviceToken()),
                message.getTitle(),
                message.getContent(),
                message.getClickAction(),
                message.getExtras());
        return true;
    }

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

    @Override
    public String getProviderName() {
        return "mock";
    }

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
