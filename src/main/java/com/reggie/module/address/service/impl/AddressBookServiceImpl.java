package com.reggie.module.address.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        AddressBook address = this.getById(id);
        if (address == null) {
            return;
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        // 先把当前用户所有地址设为非默认
        AddressBook updateAll = new AddressBook();
        updateAll.setIsDefault(AddressBook.NOT_DEFAULT);
        this.update(updateAll, new LambdaQueryWrapper<AddressBook>()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getTenantId, tenantId));
        // 再设置指定地址为默认
        AddressBook updateTarget = new AddressBook();
        updateTarget.setId(id);
        updateTarget.setIsDefault(AddressBook.IS_DEFAULT);
        this.updateById(updateTarget);
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



