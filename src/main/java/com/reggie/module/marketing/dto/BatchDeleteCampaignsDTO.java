package com.reggie.module.marketing.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量删除营销活动请求 DTO
 */
public class BatchDeleteCampaignsDTO {

    @NotNull(message = "活动ID列表不能为空")
    @Size(min = 1, message = "至少选择一个活动")
    private List<Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}

