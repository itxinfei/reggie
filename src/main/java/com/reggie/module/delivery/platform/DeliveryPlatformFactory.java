package com.reggie.module.delivery.platform;

import com.reggie.module.delivery.model.PlatformEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 配送平台工厂，根据平台类型返回对应的平台适配器实例。
 * </p>
 *
 * @author 心飞为你飞
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

    /** 抖音平台适配器 */
    @Autowired
    private DouyinAdapter douyinAdapter;

    /** 达达平台适配器 */
    @Autowired
    private DadaAdapter dadaAdapter;

    /** 蜂鸟平台适配器 */
    @Autowired
    private FengniaoAdapter fengniaoAdapter;

    /** 顺丰平台适配器 */
    @Autowired
    private ShunfengAdapter shunfengAdapter;

    /**
     * 根据平台类型获取对应的平台适配器
     *
     * @param platform 平台类型（MEITUAN/ELEME/DOUYIN/DADA/FENGNIAO/SHUNFENG）
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
            return douyinAdapter;
        }
        if ("DADA".equals(platform)) {
            return dadaAdapter;
        }
        if ("FENGNIAO".equals(platform)) {
            return fengniaoAdapter;
        }
        if ("SHUNFENG".equals(platform)) {
            return shunfengAdapter;
        }
        throw new IllegalArgumentException("不支持的外卖平台: " + platform);
    }
}
