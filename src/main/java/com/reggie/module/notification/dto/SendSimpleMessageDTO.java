package com.reggie.module.notification.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 简易消息发送请求 DTO
 */
public class SendSimpleMessageDTO {

    private Integer channel = 1;

    @NotEmpty(message = "目标用户不能为空")
    private List<String> targets;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1024, message = "消息内容长度不能超过1024")
    private String content;

    @Size(max = 64, message = "标题长度不能超过64")
    private String title;

    /**
     * 获取 channel。
     * @return 返回结果
     */
    public Integer getChannel() {
        return channel;
    }

    /**
     * 设置 channel。
     * @param channel 参数 channel
     */
    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    /**
     * 获取 targets。
     * @return 返回结果
     */
    public List<String> getTargets() {
        return targets;
    }

    /**
     * 设置 targets。
     * @param targets 参数 targets
     */
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    /**
     * 获取 content。
     * @return 返回结果
     */
    public String getContent() {
        return content;
    }

    /**
     * 设置 content。
     * @param content 参数 content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 获取 title。
     * @return 返回结果
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置 title。
     * @param title 参数 title
     */
    public void setTitle(String title) {
        this.title = title;
    }
}
