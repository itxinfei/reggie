package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Employee;
import com.reggie.entity.Tenant;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * <p>
 * 租户管理服务接口，提供租户注册、管理员创建及验证码管理功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface TenantService extends IService<Tenant> {

    /**
     * 注册新租户并创建管理员账号（事务保护）
     * @param tenant 租户信息
     * @param username 管理员用户名
     * @param password 管理员密码
     * @param phone 手机号（用于验证码校验）
     * @param verifyCode 验证码
     * @param session HttpSession（用于读取验证码）
     */
    void registerWithAdmin(Tenant tenant, String username, String password, String phone, String verifyCode, HttpSession session);

    /**
     * 保存验证码
     * @param phone 手机号
     * @param verifyCode 验证码
     * @param session HttpSession
     */
    void saveVerifyCode(String phone, String verifyCode, HttpSession session);

    /**
     * 查询所有活跃租户列表（供定时任务等多租户批处理场景使用）
     * @return 活跃租户列表
     */
    List<Tenant> listActiveTenants();
}
