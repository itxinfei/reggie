package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.User;

/**
 * <p>
 * 用户服务接口，提供用户注册、登录及信息管理功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param phone 手机号
     * @return 注册成功的用户信息
     */
    User register(String phone);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息，不存在则返回null
     */
    User getByPhone(String phone);

    /**
     * 更新用户个人信息
     *
     * @param user 用户信息
     */
    void updateUserBaseInfo(User user);
}
