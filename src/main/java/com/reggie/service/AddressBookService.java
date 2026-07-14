package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.AddressBook;

import java.util.List;

/**
 * <p>
 * 地址簿服务接口，提供地址簿的增删改查功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface AddressBookService extends IService<AddressBook> {

    /**
     * 查询当前用户的所有地址
     *
     * @return 地址列表
     */
    List<AddressBook> listByCurrentUser();

    /**
     * 设置默认地址
     *
     * @param id 地址ID
     */
    void setDefault(Long id);

    /**
     * 根据ID和当前用户查询地址
     *
     * @param id 地址ID
     * @return 地址信息
     */
    AddressBook getByIdCurrent(Long id);
}
