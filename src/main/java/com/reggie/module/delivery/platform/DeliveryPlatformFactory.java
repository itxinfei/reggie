package com.reggie.module.delivery.platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeliveryPlatformFactory {

    @Autowired
    private MeituanAdapter meituanAdapter;

    @Autowired
    private ElemeAdapter elemeAdapter;

    public DeliveryPlatform getPlatform(String platform) {
        if ("MEITUAN".equals(platform)) return meituanAdapter;
        if ("ELEME".equals(platform)) return elemeAdapter;
        if ("DOUYIN".equals(platform)) return meituanAdapter;
        throw new IllegalArgumentException("不支持的外卖平台: " + platform);
    }
}
