package com.reggie.module.recommend.dto;

/**
 * 批量推送营销消息请求 DTO
 */
public class BatchPushCampaignDTO {

    private Integer pushType = 1;

    public Integer getPushType() {
        return pushType;
    }

    public void setPushType(Integer pushType) {
        this.pushType = pushType;
    }
}
