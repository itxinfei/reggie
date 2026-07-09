package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.model.PlatformEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeliveryPlatformFactory {

    @Autowired
    private MeituanAdapter meituanAdapter;

    @Autowired
    private ElemeAdapter elemeAdapter;

    public DeliveryPlatform getPlatform(String platform) {
        if (PlatformEnum.MEITUAN.name().equals(platform)) {
            return meituanAdapter;
        }
        if (PlatformEnum.ELEME.name().equals(platform)) {
            return elemeAdapter;
        }
        if (PlatformEnum.DOUYIN.name().equals(platform)) {
            // 抖音平台暂复用美团适配器
            return meituanAdapter;
        }
        throw new IllegalArgumentException("不支持的外卖平台: " + platform);
    }
}
