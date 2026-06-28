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
}
