package com.reggie.module.auth.dto;

import javax.validation.constraints.NotNull;

/**
 * 修改员工状态请求 DTO
 */
public class UpdateEmployeeStatusDTO {

    @NotNull(message = "员工ID不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
