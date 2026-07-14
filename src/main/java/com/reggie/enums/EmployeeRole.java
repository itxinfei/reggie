package com.reggie.enums;

import lombok.Getter;

/**
 * <p>
 * 员工角色枚举
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Getter
public enum EmployeeRole {

    /**
     * 超级管理员：拥有所有权限
     */
    ADMIN(1, "超级管理员"),

    /**
     * 普通员工：基础操作权限
     */
    STAFF(2, "普通员工");

    private final int value;
    private final String desc;

    EmployeeRole(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static boolean isAdmin(Integer role) {
        return role != null && role == ADMIN.getValue();
    }

    public static boolean isStaff(Integer role) {
        return role != null && role == STAFF.getValue();
    }
}
