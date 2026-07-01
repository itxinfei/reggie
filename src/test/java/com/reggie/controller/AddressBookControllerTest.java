package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.entity.AddressBook;
import com.reggie.service.AddressBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AddressBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddressBookService addressBookService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/addressBook")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"consignee\":\"新地址联系人\",\"phone\":\"13500135000\",\"sex\":\"1\",\"provinceName\":\"广东省\",\"cityName\":\"深圳市\",\"districtName\":\"南山区\",\"detail\":\"科技园路1号\",\"label\":\"公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("新地址联系人"));
    }

    @Test
    void testUpdateAddress() throws Exception {
        AddressBook address = new AddressBook();
        address.setId(1L);
        address.setUserId(1L);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setDetail("测试地址");
        address.setLabel("家");
        addressBookService.save(address);

        mockMvc.perform(put("/addressBook")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"consignee\":\"李四\",\"phone\":\"13900139000\",\"detail\":\"新地址\",\"label\":\"公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals("李四", addressBookService.getById(1L).getConsignee());
    }

    @Test
    void testDeleteAddress() throws Exception {
        AddressBook address = new AddressBook();
        address.setId(2L);
        address.setUserId(1L);
        address.setConsignee("王五");
        address.setPhone("13700137000");
        address.setDetail("待删除地址");
        addressBookService.save(address);

        mockMvc.perform(delete("/addressBook")
                .param("ids", "2")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertNull(addressBookService.getById(2L));
    }

    @Test
    void testLastUpdate() throws Exception {
        AddressBook address = new AddressBook();
        address.setId(3L);
        address.setUserId(1L);
        address.setConsignee("赵六");
        address.setPhone("13600136000");
        address.setDetail("最近地址");
        addressBookService.save(address);

        mockMvc.perform(get("/addressBook/lastUpdate")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("赵六"));
    }

    @Test
    void testSetDefault() throws Exception {
        AddressBook addr1 = new AddressBook();
        addr1.setId(10L);
        addr1.setUserId(1L);
        addr1.setConsignee("地址一");
        addr1.setPhone("13800138000");
        addr1.setDetail("地址一详情");
        addr1.setIsDefault(1);
        addressBookService.save(addr1);

        AddressBook addr2 = new AddressBook();
        addr2.setId(11L);
        addr2.setUserId(1L);
        addr2.setConsignee("地址二");
        addr2.setPhone("13900139000");
        addr2.setDetail("地址二详情");
        addr2.setIsDefault(0);
        addressBookService.save(addr2);

        mockMvc.perform(put("/addressBook/default")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":11}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(0, addressBookService.getById(10L).getIsDefault());
        org.junit.jupiter.api.Assertions.assertEquals(1, addressBookService.getById(11L).getIsDefault());
    }

    @Test
    void testGetById() throws Exception {
        AddressBook address = new AddressBook();
        address.setId(4L);
        address.setUserId(1L);
        address.setConsignee("查询联系人");
        address.setPhone("13500135000");
        address.setDetail("查询地址");
        addressBookService.save(address);

        mockMvc.perform(get("/addressBook/4")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("查询联系人"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/addressBook/999")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testGetDefault() throws Exception {
        AddressBook address = new AddressBook();
        address.setId(5L);
        address.setUserId(1L);
        address.setConsignee("默认地址");
        address.setPhone("13800138000");
        address.setDetail("默认地址详情");
        address.setIsDefault(1);
        addressBookService.save(address);

        mockMvc.perform(get("/addressBook/default")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("默认地址"));
    }

    @Test
    void testGetDefaultNotFound() throws Exception {
        mockMvc.perform(get("/addressBook/default")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testList() throws Exception {
        AddressBook addr1 = new AddressBook();
        addr1.setId(6L);
        addr1.setUserId(1L);
        addr1.setConsignee("地址一");
        addr1.setPhone("13800138000");
        addr1.setDetail("地址一详情");
        addressBookService.save(addr1);

        AddressBook addr2 = new AddressBook();
        addr2.setId(7L);
        addr2.setUserId(1L);
        addr2.setConsignee("地址二");
        addr2.setPhone("13900139000");
        addr2.setDetail("地址二详情");
        addressBookService.save(addr2);

        mockMvc.perform(get("/addressBook/list")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
