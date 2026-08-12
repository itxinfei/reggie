package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
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

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
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
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/addressBook")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"consignee\":\"新地址联系人\",\"phone\":\"13500135000\",\"sex\":\"1\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"detail\":\"科技园路1号\",\"label\":\"公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("新地址联系人"));
    }

    @Test
    void testUpdateAddress() throws Exception {
        AddressBook address = new AddressBook();
        address.setUserId(1L);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setProvinceCode("440000");
        address.setProvinceName("广东省");
        address.setCityCode("440300");
        address.setCityName("深圳市");
        address.setDistrictCode("440305");
        address.setDistrictName("南山区");
        address.setDetail("测试地址");
        address.setLabel("家");
        addressBookService.save(address);
        long generatedId = address.getId();

        mockMvc.perform(put("/addressBook")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + generatedId + ",\"userId\":1,\"consignee\":\"李四\",\"phone\":\"13900139000\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"detail\":\"新地址\",\"label\":\"公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals("李四", addressBookService.getById(generatedId).getConsignee());
    }

    @Test
    void testDeleteAddress() throws Exception {
        AddressBook address = new AddressBook();
        address.setUserId(1L);
        address.setConsignee("王五");
        address.setPhone("13700137000");
        address.setDetail("待删除地址");
        addressBookService.save(address);
        long generatedId = address.getId();

        mockMvc.perform(delete("/addressBook")
                .param("ids", String.valueOf(generatedId))
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertNull(addressBookService.getById(generatedId));
    }

    @Test
    void testLastUpdate() throws Exception {
        AddressBook address = new AddressBook();
        address.setUserId(1L);
        address.setConsignee("赵六");
        address.setPhone("13600136000");
        address.setDetail("最近地址");
        addressBookService.save(address);

        mockMvc.perform(get("/addressBook/lastUpdate")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("赵六"));
    }

    @Test
    void testSetDefault() throws Exception {
        AddressBook addr1 = new AddressBook();
        addr1.setUserId(1L);
        addr1.setConsignee("地址一");
        addr1.setPhone("13800138000");
        addr1.setDetail("地址一详情");
        addr1.setIsDefault(1);
        addressBookService.save(addr1);
        long id1 = addr1.getId();

        AddressBook addr2 = new AddressBook();
        addr2.setUserId(1L);
        addr2.setConsignee("地址二");
        addr2.setPhone("13900139000");
        addr2.setDetail("地址二详情");
        addr2.setIsDefault(0);
        addressBookService.save(addr2);
        long id2 = addr2.getId();

        mockMvc.perform(put("/addressBook/default")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + id2 + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(0, addressBookService.getById(id1).getIsDefault());
        org.junit.jupiter.api.Assertions.assertEquals(1, addressBookService.getById(id2).getIsDefault());
    }

    @Test
    void testGetById() throws Exception {
        AddressBook address = new AddressBook();
        address.setUserId(1L);
        address.setConsignee("查询联系人");
        address.setPhone("13500135000");
        address.setDetail("查询地址");
        addressBookService.save(address);
        long generatedId = address.getId();

        mockMvc.perform(get("/addressBook/" + generatedId)
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("查询联系人"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/addressBook/999")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testGetDefault() throws Exception {
        AddressBook address = new AddressBook();
        address.setUserId(1L);
        address.setConsignee("默认地址");
        address.setPhone("13800138000");
        address.setDetail("默认地址详情");
        address.setIsDefault(1);
        addressBookService.save(address);

        mockMvc.perform(get("/addressBook/default")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("默认地址"));
    }

    @Test
    void testGetDefaultNotFound() throws Exception {
        mockMvc.perform(get("/addressBook/default")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testList() throws Exception {
        AddressBook addr1 = new AddressBook();
        addr1.setUserId(1L);
        addr1.setConsignee("地址一");
        addr1.setPhone("13800138000");
        addr1.setDetail("地址一详情");
        addressBookService.save(addr1);

        AddressBook addr2 = new AddressBook();
        addr2.setUserId(1L);
        addr2.setConsignee("地址二");
        addr2.setPhone("13900139000");
        addr2.setDetail("地址二详情");
        addressBookService.save(addr2);

        mockMvc.perform(get("/addressBook/list")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}



