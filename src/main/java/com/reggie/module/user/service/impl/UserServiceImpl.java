package com.reggie.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.PasswordUtils;
import com.reggie.module.user.model.User;
import com.reggie.module.user.mapper.UserMapper;
import com.reggie.module.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User register(String phone) {
        Long tenantId = BaseContext.getCurrentTenantId();
        // 检查手机号是否已存在
        User existUser = this.getByPhone(phone);
        if (existUser != null) {
            return existUser;
        }
        User user = new User();
        user.setPhone(phone);
        user.setTenantId(tenantId);
        user.setStatus(1);
        this.save(user);
        return user;
    }

    @Override
    public User getByPhone(String phone) {
        return this.list(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .eq(User::getTenantId, BaseContext.getCurrentTenantId())
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    @Override
    public void updateUserBaseInfo(User user) {
        User updateEntity = new User();
        if (user.getName() != null) {
            updateEntity.setName(user.getName());
        }
        if (user.getSex() != null) {
            updateEntity.setSex(user.getSex());
        }
        if (user.getIdNumber() != null) {
            updateEntity.setIdNumber(user.getIdNumber());
        }
        if (user.getAvatar() != null) {
            updateEntity.setAvatar(user.getAvatar());
        }
        if (user.getId() != null) {
            updateEntity.setId(user.getId());
            this.updateById(updateEntity);
        }
    }
}



