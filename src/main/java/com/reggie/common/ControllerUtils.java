package com.reggie.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 控制器工具类
 *
 * @author reggie
 * @since 2026-07-15
 */
public final class ControllerUtils {

    private ControllerUtils() {}

    /**
     * 解析逗号分隔的ID字符串为Long列表
     * 兼容前端传递的格式：单个ID("1")、逗号分隔("1,2,3")、数组("1&ids=2")
     *
     * @param ids 逗号分隔的ID字符串
     * @return ID列表，空输入返回空列表
     */
    public static List<Long> parseIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
