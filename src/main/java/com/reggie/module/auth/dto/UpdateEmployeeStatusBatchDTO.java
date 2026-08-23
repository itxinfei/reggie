package com.reggie.module.auth.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量修改员工状态请求 DTO
 */
public class UpdateEmployeeStatusBatchDTO {

    @NotNull(message = "员工ID列表不能为空")
    @Size(min = 1, message = "至少选择一个员工")
    private List<Long> ids;

    @NotNull(message = "状态不能为空")
    private Integer status;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
