package com.reggie.module.store.model;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    private String keyword;

    /** 门店类型：1-直营总店 2-直营分店 3-加盟店 */
    @Min(value = 1, message = "门店类型无效")
    @Max(value = 3, message = "门店类型无效")
    private Integer storeType;

    /** 门店状态：null-全部 1-启用 0-停用 */
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    /** 页码，默认1 */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /** 每页条数，默认10 */
    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;

    /** 排序字段：storeName / todayOrders / todayAmount / createTime */
    @Pattern(regexp = "^(storeName|todayOrders|todayAmount|createTime)$", message = "排序字段无效")
    private String sortBy;

    /** 排序方向：asc / desc */
    @Pattern(regexp = "^(asc|desc)$", message = "排序方向必须为asc或desc")
    private String sortOrder = "desc";
}
