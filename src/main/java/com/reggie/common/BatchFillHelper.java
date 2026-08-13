package com.reggie.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 批量名称回填工具类
 * <p>
 * 解决多个 Service 中重复出现的"根据 ID 批量查询名称并回填"模式，
 * 如 fillSupplierName、fillMaterialName、fillLevelName 等。
 *
 * @author reggie
 * @since 2026-08-10
 */
public final class BatchFillHelper {

    private BatchFillHelper() {}

    /**
     * 批量回填关联名称
     *
     * @param list         需要回填的记录列表
     * @param idGetter     从记录中提取关联 ID 的函数
     * @param idBatchQuery 批量查询关联对象的函数（输入 ID 集合，返回 ID→Name 映射）
     * @param nameSetter   将名称设置回记录的消费者
     * @param <T>          记录类型
     * @param <ID>         关联 ID 类型
     */
    public static <T, ID> void fillNames(
            List<T> list,
            Function<T, ID> idGetter,
            Function<Set<ID>, Map<ID, String>> idBatchQuery,
            BiConsumer<T, String> nameSetter) {

        if (list == null || list.isEmpty()) {
            return;
        }

        // 收集所有关联 ID（去重、过滤 null）
        Set<ID> ids = list.stream()
                .map(idGetter)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) {
            return;
        }

        // 批量查询名称映射
        Map<ID, String> nameMap = idBatchQuery.apply(ids);

        // 回填名称
        for (T item : list) {
            ID id = idGetter.apply(item);
            if (id != null) {
                nameSetter.accept(item, nameMap.get(id));
            }
        }
    }
}
