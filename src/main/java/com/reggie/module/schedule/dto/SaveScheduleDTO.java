package com.reggie.module.schedule.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 保存排班请求 DTO
 */
public class SaveScheduleDTO {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    @NotNull(message = "日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为 yyyy-MM-dd")
    private String date;

    private Integer shift = 0;

    @Size(max = 10, message = "班次开始时间长度不能超过10")
    private String shiftStart = "08:00";

    @Size(max = 10, message = "班次结束时间长度不能超过10")
    private String shiftEnd = "20:00";

    /**
     * 获取 employee id。
     * @return 返回结果
     */
    public Long getEmployeeId() {
        return employeeId;
    }

    /**
     * 设置 employee id。
     * @param employeeId 参数 employeeId
     */
    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    /**
     * 获取 date。
     * @return 返回结果
     */
    public String getDate() {
        return date;
    }

    /**
     * 设置 date。
     * @param date 参数 date
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * 获取 shift。
     * @return 返回结果
     */
    public Integer getShift() {
        return shift;
    }

    /**
     * 设置 shift。
     * @param shift 参数 shift
     */
    public void setShift(Integer shift) {
        this.shift = shift;
    }

    /**
     * 获取 shift start。
     * @return 返回结果
     */
    public String getShiftStart() {
        return shiftStart;
    }

    /**
     * 设置 shift start。
     * @param shiftStart 参数 shiftStart
     */
    public void setShiftStart(String shiftStart) {
        this.shiftStart = shiftStart;
    }

    /**
     * 获取 shift end。
     * @return 返回结果
     */
    public String getShiftEnd() {
        return shiftEnd;
    }

    /**
     * 设置 shift end。
     * @param shiftEnd 参数 shiftEnd
     */
    public void setShiftEnd(String shiftEnd) {
        this.shiftEnd = shiftEnd;
    }
}
