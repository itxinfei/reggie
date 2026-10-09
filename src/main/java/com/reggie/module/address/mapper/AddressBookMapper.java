package com.reggie.module.address.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.address.model.AddressBook;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 地址簿 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {

}

