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

    /**
     * 获取 id。
     * @return 返回结果
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置 id。
     * @param id 参数 id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取 status。
     * @return 返回结果
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置 status。
     * @param status 参数 status
     */
    public void setStatus(Integer status) {
        this.status = status;
    }
}
