package com.reggie.module.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 工具注册表：Spring 自动收集所有 {@link AiTool} Bean，按 name 索引，
 * 并输出协议无关的 {@link ToolDefinition} 列表供编排层发送给模型。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@Component
public class AiToolRegistry {

    @Resource
    private List<AiTool> toolBeans;

    private final Map<String, AiTool> tools = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        if (toolBeans == null) {
            return;
        }
        for (AiTool tool : toolBeans) {
            AiTool old = tools.put(tool.name(), tool);
            if (old != null) {
                log.warn("AI工具名冲突，后注册者覆盖: name={}, old={}, new={}",
                        tool.name(), old.getClass().getSimpleName(), tool.getClass().getSimpleName());
            }
        }
        log.info("AI经营数据工具注册完成: {} 个 -> {}", tools.size(), tools.keySet());
    }

    /** 按名取工具，未注册返回 null */
    public AiTool get(String name) {
        return name == null ? null : tools.get(name);
    }

    /** 全部工具定义（声明顺序稳定，便于模型稳定选择） */
    public List<ToolDefinition> definitions() {
        List<ToolDefinition> list = new ArrayList<>();
        for (AiTool tool : tools.values()) {
            list.add(new ToolDefinition(tool.name(), tool.description(), tool.parametersSchema()));
        }
        return Collections.unmodifiableList(list);
    }

    /** 工具中文名（事件展示用，未知工具回退英文 name） */
    public String labelOf(String name) {
        AiTool tool = get(name);
        return tool != null ? tool.label() : name;
    }
}
