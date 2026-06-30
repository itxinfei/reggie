package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.entity.AddressBook;
import com.reggie.service.AddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿管理
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
@Tag(name = "地址簿管理", description = "用户地址簿CRUD接口")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增
     */
    @PostMapping
    @Operation(summary = "新增地址", description = "添加新的收货地址")
    @Parameter(name = "addressBook", description = "地址信息", required = true)
    public R<AddressBook> save(@RequestBody AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("新增地址，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));
        addressBookService.save(addressBook);
        return R.success(addressBook);
    }

    @PutMapping
    @Operation(summary = "修改地址", description = "更新地址信息")
    @Parameter(name = "addressBook", description = "地址信息", required = true)
    public R<AddressBook> update(@RequestBody AddressBook addressBook) {
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }

    @DeleteMapping
    @Operation(summary = "删除地址", description = "批量删除地址")
    @Parameter(name = "ids", description = "地址ID列表", required = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        addressBookService.removeByIds(ids);
        return R.success("删除成功");
    }

    @GetMapping("/lastUpdate")
    @Operation(summary = "查询最后更新的地址", description = "查询用户最近更新的地址")
    public R<AddressBook> lastUpdate() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);
        queryWrapper.last("LIMIT 1");
        AddressBook addressBook = addressBookService.getOne(queryWrapper);
        if (addressBook != null) {
            return R.success(addressBook);
        }
        return R.error("没有找到该对象");
    }

    /**
     * 设置默认地址
     */
    @PutMapping("default")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认收货地址")
    @Parameter(name = "addressBook", description = "地址信息", required = true)
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook) {
        log.info("设置默认地址，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));
        LambdaUpdateWrapper<AddressBook> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        wrapper.set(AddressBook::getIsDefault, 0);
        //SQL:update address_book set is_default = 0 where user_id = ?
        addressBookService.update(wrapper);

        addressBook.setIsDefault(1);
        //SQL:update address_book set is_default = 1 where id = ?
        addressBookService.updateById(addressBook);
        return R.success(addressBook);
    }

    /**
     * 根据id查询地址
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询地址详情", description = "根据ID查询地址信息")
    @Parameter(name = "id", description = "地址ID", required = true)
    public R get(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("没有找到该对象");
        }
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    @Operation(summary = "查询默认地址", description = "查询用户的默认收货地址")
    public R<AddressBook> getDefault() {
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getCurrentId());
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        if (null == addressBook) {
            return R.error("没有找到该对象");
        } else {
            return R.success(addressBook);
        }
    }

    /**
     * 查询指定用户的全部地址
     */
    @GetMapping("/list")
    @Operation(summary = "查询地址列表", description = "查询用户的所有地址")
    @Parameter(name = "addressBook", description = "地址查询条件")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        log.info("查询地址列表，手机号：{}，地址：{}",
            LogMaskUtils.maskPhone(addressBook.getPhone()),
            LogMaskUtils.maskAddress(addressBook.getDetail()));

        //条件构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(null != addressBook.getUserId(), AddressBook::getUserId, addressBook.getUserId());
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //SQL:select * from address_book where user_id = ? order by update_time desc
        return R.success(addressBookService.list(queryWrapper));
    }
}
