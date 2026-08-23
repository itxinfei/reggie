package com.reggie.module.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.mapper.AddressBookMapper;
import com.reggie.module.address.service.AddressBookService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 地址簿服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {

    @Override
    public List<AddressBook> listByCurrentUser() {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        return this.list(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getTenantId, tenantId)
                .orderByDesc(AddressBook::getCreateTime));
    }

    @Override
    public void setDefault(Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        // 先按租户和用户过滤，确认地址归属
        AddressBook address = this.getOne(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getId, id)
                .eq(AddressBook::getTenantId, tenantId)
                .eq(AddressBook::getUserId, userId));
        if (address == null) {
            return;
        }
        // 先把当前用户同租户所有地址设为非默认
        LambdaUpdateWrapper<AddressBook> resetWrapper = new LambdaUpdateWrapper<>();
        resetWrapper.eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getTenantId, tenantId)
                .set(AddressBook::getIsDefault, AddressBook.NOT_DEFAULT);
        this.update(resetWrapper);
        // 再设置指定地址为默认（租户+用户双条件，防止跨租户越权）
        LambdaUpdateWrapper<AddressBook> setWrapper = new LambdaUpdateWrapper<>();
        setWrapper.eq(AddressBook::getId, id)
                .eq(AddressBook::getTenantId, tenantId)
                .eq(AddressBook::getUserId, userId)
                .set(AddressBook::getIsDefault, AddressBook.IS_DEFAULT);
        this.update(setWrapper);
    }

    @Override
    public AddressBook getByIdCurrent(Long id) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        return this.list(new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getId, id)
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getTenantId, tenantId))
                .stream().findFirst().orElse(null);
    }
}



