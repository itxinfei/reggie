package com.reggie.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应封装
 *
 * @param <T> 数据类型
 * @author reggie
 * @since 2026-08-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应结果")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "每页大小")
    private Long size;

    @Schema(description = "当前页码")
    private Long current;

    /**
     * 创建空的分页结果
     */
    public static <T> PageResult<T> empty(long size, long current) {
        return new PageResult<>(Collections.emptyList(), 0L, size, current);
    }

    /**
     * 从 MyBatis-Plus Page 对象转换
     */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getSize(), page.getCurrent());
    }
}
