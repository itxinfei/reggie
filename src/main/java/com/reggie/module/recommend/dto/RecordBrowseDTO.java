package com.reggie.module.recommend.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 记录用户浏览行为请求 DTO
 */
public class RecordBrowseDTO {

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    private Long targetId;

    @Size(max = 256, message = "目标名称长度不能超过256")
    private String targetName;

    private Integer duration = 0;

    private Integer actionType = 1;

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getActionType() {
        return actionType;
    }

    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }
}
