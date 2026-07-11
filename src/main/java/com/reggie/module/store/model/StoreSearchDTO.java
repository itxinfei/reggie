package com.reggie.module.store.model;

import lombok.Data;

/**
 * 门店搜索请求参数 DTO
 * 支持多条件筛选、分页、排序
 *
 * @author reggie
 * @since 2026-07-11
 */
@Data
public class StoreSearchDTO {

    /** 关键词（门店名称或编码模糊匹配） */
    private String keyword;

    /** 门店类型：1-直营总店 2-直营分店 3-加盟店 */
    private Integer storeType;

    /** 门店状态：null-全部 1-启用 0-停用 */
    private Integer status;

    /** 页码，默认1 */
    private Integer page = 1;

    /** 每页条数，默认10 */
    private Integer pageSize = 10;

    /** 排序字段：storeName / todayOrders / todayAmount / createTime */
    private String sortBy;

    /** 排序方向：asc / desc */
    private String sortOrder = "desc";
}
