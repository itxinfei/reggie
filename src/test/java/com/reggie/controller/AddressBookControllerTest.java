package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AddressBookControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("address_book");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"consignee\":\"新地址联系人\",\"phone\":\"13500135000\",\"sex\":\"1\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"streetName\":\"粤海街道\",\"community\":\"科技园小区\",\"building\":\"3栋\",\"unit\":\"2单元\",\"floor\":\"15层\",\"roomNo\":\"1503室\",\"label\":\"公司\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("新地址联系人"))
                // 结构化字段独立保存，detail 由后端按字段规范化拼接
                .andExpect(jsonPath("$.data.community").value("科技园小区"))
                .andExpect(jsonPath("$.data.roomNo").value("1503室"))
                .andExpect(jsonPath("$.data.detail").value(org.hamcrest.Matchers.containsString("科技园小区")));
    }

    @Test
    void testSaveWithoutDetailRejected() throws Exception {
        // 只选省市区、不填任何结构化详细信息：应被拦截
        mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"consignee\":\"张三\",\"phone\":\"13500135000\",\"sex\":\"1\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"label\":\"家\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("小区")));
    }

    @Test
    void testSaveNumericAutoSuffix() throws Exception {
        // 楼栋/单元/层/门牌只填纯数字：后端补标准后缀，detail 规范化拼接
        mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":1,\"consignee\":\"张三\",\"phone\":\"13500135000\",\"sex\":\"1\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"community\":\"科技园小区\",\"building\":\"3\",\"unit\":\"2\",\"floor\":\"15\",\"roomNo\":\"1503\",\"label\":\"家\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.detail").value("科技园小区3栋2单元15层1503室"));
    }

    @Test
    void testGetCrossUserRejected() throws Exception {
        // 用户1 的地址
        AddressBook addr = buildUserAddress(1L, "幸福里小区");
        long id = addr.getId();
        // 切换为同租户的用户2：不得读取用户1 的地址（PII）
        BaseContext.setCurrentId(2L);
        mockMvc.perform(get("/address-book/" + id)
                .sessionAttr("user", 2L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("没有查询到")));
        BaseContext.setCurrentId(1L);
    }

    @Test
    void testUpdateCrossUserRejected() throws Exception {
        AddressBook addr = buildUserAddress(1L, "幸福里小区");
        long id = addr.getId();
        // 用户2 尝试篡改用户1 的地址：拒绝且数据不变
        BaseContext.setCurrentId(2L);
        mockMvc.perform(withCsrfToken(mockMvc, put("/address-book")
                .sessionAttr("user", 2L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + id + ",\"consignee\":\"李四\",\"phone\":\"13900139000\","
                        + "\"provinceCode\":\"440000\",\"provinceName\":\"广东省\","
                        + "\"cityCode\":\"440300\",\"cityName\":\"深圳市\","
                        + "\"districtCode\":\"440305\",\"districtName\":\"南山区\","
                        + "\"community\":\"被篡改小区\",\"building\":\"9\",\"roomNo\":\"9999\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        BaseContext.setCurrentId(1L);
        org.junit.jupiter.api.Assertions.assertEquals("幸福里小区",
                addressBookService.getById(id).getCommunity());
    }

    @Test
    void testTextBuildingNoAutoSuffix() throws Exception {
        // 楼栋填文本"东门"：不应被补成"东门栋"
        mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"consignee\":\"张三\",\"phone\":\"13800138000\",\"sex\":\"1\","
                        + "\"provinceCode\":\"440000\",\"provinceName\":\"广东省\","
                        + "\"cityCode\":\"440300\",\"cityName\":\"深圳市\","
                        + "\"districtCode\":\"440305\",\"districtName\":\"南山区\","
                        + "\"community\":\"科技园小区\",\"building\":\"东门\",\"label\":\"家\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.detail").value("科技园小区东门"));
    }

    @Test
    void testLegacyPartialUpdateBlocked() throws Exception {
        // 旧格式地址（六结构化列空、只有完整 detail）
        AddressBook legacy = new AddressBook();
        legacy.setUserId(1L);
        legacy.setTenantId(1L);
        legacy.setConsignee("张三");
        legacy.setPhone("13800138000");
        legacy.setProvinceCode("440000");
        legacy.setProvinceName("广东省");
        legacy.setCityCode("440300");
        legacy.setCityName("深圳市");
        legacy.setDistrictCode("440305");
        legacy.setDistrictName("南山区");
        legacy.setDetail("科技园路1号幸福里3栋2单元1503室");
        addressBookService.save(legacy);
        long id = legacy.getId();
        // 仅补一个零碎门牌（无小区、无栋）：阻断，不得用"1503室"覆盖原完整地址
        mockMvc.perform(withCsrfToken(mockMvc, put("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + id + ",\"consignee\":\"张三\",\"phone\":\"13800138000\","
                        + "\"provinceCode\":\"440000\",\"provinceName\":\"广东省\","
                        + "\"cityCode\":\"440300\",\"cityName\":\"深圳市\","
                        + "\"districtCode\":\"440305\",\"districtName\":\"南山区\","
                        + "\"streetName\":\"\",\"community\":\"\",\"building\":\"\","
                        + "\"unit\":\"\",\"floor\":\"\",\"roomNo\":\"1503\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        org.junit.jupiter.api.Assertions.assertTrue(
                addressBookService.getById(id).getDetail().contains("科技园路"));
    }

    @Test
    void testLongStructuredTruncatedNotFail() throws Exception {
        // 各字段接近上限：detail 理论拼接超 255，后端截断兜底，不得抛 500
        String community = repeatChars('幸', 100);
        String street = repeatChars('街', 50);
        mockMvc.perform(withCsrfToken(mockMvc, post("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"consignee\":\"张三\",\"phone\":\"13800138000\",\"sex\":\"1\","
                        + "\"provinceCode\":\"440000\",\"provinceName\":\"广东省\","
                        + "\"cityCode\":\"440300\",\"cityName\":\"深圳市\","
                        + "\"districtCode\":\"440305\",\"districtName\":\"南山区\","
                        + "\"streetName\":\"" + street + "\",\"community\":\"" + community + "\","
                        + "\"building\":\"3\",\"unit\":\"2\",\"floor\":\"15\",\"roomNo\":\"1503\","
                        + "\"label\":\"家\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        AddressBook saved = addressBookService.lambdaQuery()
                .eq(AddressBook::getUserId, 1L)
                .orderByDesc(AddressBook::getId).last("LIMIT 1").one();
        org.junit.jupiter.api.Assertions.assertNotNull(saved);
        org.junit.jupiter.api.Assertions.assertTrue(saved.getDetail().length() <= 255);
    }

    private AddressBook buildUserAddress(long userId, String community) {
        AddressBook addr = new AddressBook();
        addr.setUserId(userId);
        addr.setTenantId(1L);
        addr.setConsignee("张三");
        addr.setPhone("13800138000");
        addr.setProvinceCode("440000");
        addr.setProvinceName("广东省");
        addr.setCityCode("440300");
        addr.setCityName("深圳市");
        addr.setDistrictCode("440305");
        addr.setDistrictName("南山区");
        addr.setCommunity(community);
        addr.setDetail(community);
        addressBookService.save(addr);
        return addr;
    }

    private static String repeatChars(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
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

        mockMvc.perform(withCsrfToken(mockMvc, put("/address-book")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + generatedId + ",\"userId\":1,\"consignee\":\"李四\",\"phone\":\"13900139000\",\"provinceCode\":\"440000\",\"provinceName\":\"广东省\",\"cityCode\":\"440300\",\"cityName\":\"深圳市\",\"districtCode\":\"440305\",\"districtName\":\"南山区\",\"detail\":\"新地址\",\"label\":\"公司\"}")))
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

        mockMvc.perform(withCsrfToken(mockMvc, delete("/address-book")
                .param("ids", String.valueOf(generatedId))
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)))
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

        mockMvc.perform(get("/address-book/lastUpdate")
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

        mockMvc.perform(withCsrfToken(mockMvc, put("/address-book/default")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + id2 + "}")))
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

        mockMvc.perform(get("/address-book/" + generatedId)
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("查询联系人"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/address-book/999")
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

        mockMvc.perform(get("/address-book/default")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.consignee").value("默认地址"));
    }

    @Test
    void testGetDefaultNotFound() throws Exception {
        mockMvc.perform(get("/address-book/default")
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

        mockMvc.perform(get("/address-book/list")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}



