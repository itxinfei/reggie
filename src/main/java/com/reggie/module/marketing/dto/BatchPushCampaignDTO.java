package com.reggie.module.marketing.dto;

/**
 * 批量推送营销消息请求 DTO
 */
public class BatchPushCampaignDTO {

    private Integer pushType = 1;

    /**
     * 获取 push type。
     * @return 返回结果
     */
    public Integer getPushType() {
        return pushType;
    }

    /**
     * 设置 push type。
     * @param pushType 参数 pushType
     */
    public void setPushType(Integer pushType) {
        this.pushType = pushType;
    }
}

