package com.reggie.common.utils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页参数归一化工具。
 * <p>
 * 用于统一收敛 Controller 分页接口的 pageSize 上限与默认值，避免：
 * 1. pageSize 被传入超大值（如 10000）拖垮数据库；
 * 2. page/pageSize 未传参或传 0 时 {@code new Page<>(page, pageSize)} 产生异常分页。
 * </p>
 *
 * @author AI
 * @since 2026-07-20
 */
public final class PageUtils {

    /** 单页最大条数，防止 pageSize 过大拖垮数据库。 */
    public static final int MAX_PAGE_SIZE = 100;
    /** 默认页码。 */
    public static final int DEFAULT_PAGE = 1;
    /** 默认每页条数。 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private PageUtils() {
    }

    /**
     * 归一化分页参数并构造 MyBatis-Plus 分页对象。
     * <ul>
     *     <li>page &le; 0 时取 {@link #DEFAULT_PAGE}</li>
     *     <li>pageSize &le; 0 时取 {@link #DEFAULT_PAGE_SIZE}</li>
     *     <li>pageSize &gt; {@link #MAX_PAGE_SIZE} 时截断为上限</li>
     * </ul>
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param <T>      实体类型
     * @return 归一化后的分页对象
     */
    public static <T> Page<T> of(int page, int pageSize) {
        int safePage = page <= 0 ? DEFAULT_PAGE : page;
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(safePage, safeSize);
    }

    /**
     * 仅对每页条数做上限与默认值保护，用于 pageSize 透传给 Service 的场景。
     *
     * @param pageSize 每页条数
     * @return 归一化后的每页条数
     */
    public static int cap(int pageSize) {
        return pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
