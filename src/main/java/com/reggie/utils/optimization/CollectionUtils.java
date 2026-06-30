package com.reggie.utils.optimization;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合转换工具类
 */
public class CollectionUtils {

    /**
     * 类型安全转换（去除警告）
     * @param list 原始列表
     * @param <T> 元素类型
     * @return 转换后的列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(List<?> list) {
        return (List<T>) list;
    }

    /**
     * 提取对象列表中的ID列表
     * @param list 对象列表
     * @param getIdFunction 获取ID的函数
     * @param <T> 对象类型
     * @param <ID> ID类型
     * @return ID列表
     */
    public static <T, ID> List<ID> mapToIds(List<T> list, Function<T, ID> getIdFunction) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(getIdFunction)
                .collect(Collectors.toList());
    }

    /**
     * 通用映射转换
     * @param list 原始列表
     * @param mapper 映射函数
     * @param <T> 源类型
     * @param <R> 目标类型
     * @return 转换后的列表
     */
    public static <T, R> List<R> mapTo(List<T> list, Function<T, R> mapper) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
