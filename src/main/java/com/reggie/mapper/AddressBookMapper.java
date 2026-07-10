package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址簿Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {

}
