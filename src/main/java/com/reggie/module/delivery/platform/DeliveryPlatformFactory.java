package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.model.PlatformEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 配送平台工厂
 * 根据平台类型返回对应的平台适配器实例
 *
 * @author reggie
 * @since 2026-07-09
 */
@Component
public class DeliveryPlatformFactory {

    /** 美团平台适配器 */
    @Autowired
    private MeituanAdapter meituanAdapter;

    /** 饿了么平台适配器 */
    @Autowired
    private ElemeAdapter elemeAdapter;

    /**
     * 根据平台类型获取对应的平台适配器
     *
     * @param platform 平台类型（MEITUAN/ELEME/DOUYIN）
     * @return 平台适配器实例
     * @throws IllegalArgumentException 当平台类型不支持时抛出
     */
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
