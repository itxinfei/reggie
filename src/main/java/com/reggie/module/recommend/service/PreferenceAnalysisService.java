package com.reggie.module.recommend.service;

/**
 * 用户偏好分析服务
 * 基于订单历史和浏览记录分析用户口味/品类偏好
 */
public interface PreferenceAnalysisService {

    /**
     * 分析用户偏好并更新偏好标签
     * 基于最近30天订单数据分析用户口味偏好、品类偏好、价格偏好
     *
     * @param userId 用户ID
     * @return 分析是否成功
     */
    boolean analyzeUserPreferences(Long userId);

    /**
     * 分析用户价格偏好区间
     * 基于历史订单金额分布：经济型(<20)、实惠型(20-40)、中档(40-80)、高端(>80)
     *
     * @param userId 用户ID
     * @return 价格偏好标签名
     */
    String analyzePricePreference(Long userId);

    /**
     * 分析用户点餐时段偏好
     * 早餐(6-10)、午餐(10-14)、下午茶(14-17)、晚餐(17-21)、夜宵(21-24)
     *
     * @param userId 用户ID
     * @return 时段偏好标签名
     */
    String analyzeTimePreference(Long userId);

    /**
     * 判断用户是否为流失预警用户
     * 标准：最近30天无订单，且最近7天有浏览记录但无下单
     *
     * @param userId 用户ID
     * @return true: 流失预警
     */
    boolean isChurnWarningUser(Long userId);

    /**
     * 判断用户是否为高频用户
     * 标准：最近30天下单>=8单
     *
     * @param userId 用户ID
     * @return true: 高频用户
     */
    boolean isHighFrequencyUser(Long userId);
}
