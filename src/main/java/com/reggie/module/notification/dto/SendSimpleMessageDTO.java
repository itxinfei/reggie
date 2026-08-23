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

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
