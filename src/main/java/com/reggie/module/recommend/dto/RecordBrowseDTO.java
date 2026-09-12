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

    /**
     * 获取 target type。
     * @return 返回结果
     */
    public Integer getTargetType() {
        return targetType;
    }

    /**
     * 设置 target type。
     * @param targetType 参数 targetType
     */
    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    /**
     * 获取 target id。
     * @return 返回结果
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 设置 target id。
     * @param targetId 参数 targetId
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    /**
     * 获取 target name。
     * @return 返回结果
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * 设置 target name。
     * @param targetName 参数 targetName
     */
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    /**
     * 获取 duration。
     * @return 返回结果
     */
    public Integer getDuration() {
        return duration;
    }

    /**
     * 设置 duration。
     * @param duration 参数 duration
     */
    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * 获取 action type。
     * @return 返回结果
     */
    public Integer getActionType() {
        return actionType;
    }

    /**
     * 设置 action type。
     * @param actionType 参数 actionType
     */
    public void setActionType(Integer actionType) {
        this.actionType = actionType;
    }
}
