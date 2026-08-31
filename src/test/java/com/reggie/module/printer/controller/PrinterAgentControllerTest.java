package com.reggie.module.printer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.mapper.PrintTerminalMapper;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.model.PrintTerminal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 打印代理接口全链路测试（门店 PC 本地打印）
 *
 * <p>覆盖：注册（register）→ 心跳拉取（heartbeat）→ 打印回执（callback），
 * 以及未知门店、错误 token 等异常分支。代理接口为匿名公开接口（无登录会话）。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema-printer.sql", "classpath:printer-agent-test-data.sql"})
public class PrinterAgentControllerTest {

    private static final String STORE_CODE = "S0001";
    private static final String TERMINAL_CODE = "T-AGENT-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrintTerminalMapper printTerminalMapper;

    @Autowired
    private PrintTaskMapper printTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 注册终端并返回响应 data（terminalId / token / status）。
     */
    private JsonNode register() throws Exception {
        MvcResult result = mockMvc.perform(post("/printer/agent/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeCode\":\"" + STORE_CODE + "\",\"terminalCode\":\""
                                + TERMINAL_CODE + "\",\"name\":\"收银台\",\"printerName\":\"EPSON TM-T88V\","
                                + "\"paperSize\":\"80mm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    @Test
    void testRegister() throws Exception {
        JsonNode data = register();
        assertTrue(data.has("terminalId"));
        assertTrue(data.has("token"));
        assertFalse(data.get("token").asText().isEmpty());
        // 新终端默认停用（需管理员启用后才派发任务）
        assertEquals(0, data.get("status").asInt());
        // 新终端默认接收全部打印类型（空=全部），否则外卖单 DELIVERY/后厨单 KITCHEN 不会派发
        PrintTerminal saved = printTerminalMapper.findByTerminalCode(TERMINAL_CODE);
        assertEquals("", saved.getPrintTypes());
    }

    @Test
    void testRegisterUnknownStore() throws Exception {
        mockMvc.perform(post("/printer/agent/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeCode\":\"NO-SUCH-STORE\",\"terminalCode\":\""
                                + TERMINAL_CODE + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testHeartbeatAndCallback() throws Exception {
        JsonNode data = register();
        long terminalId = data.get("terminalId").asLong();
        String token = data.get("token").asText();

        // 管理员启用终端
        PrintTerminal terminal = printTerminalMapper.findByIdIgnoreTenant(terminalId);
        terminal.setStatus(1);
        terminal.setUpdateTime(LocalDateTime.now());
        printTerminalMapper.updateIgnoreTenant(terminal);

        // 预置一条 PENDING 任务（等价于订单打印入队）
        PrintTask task = new PrintTask();
        task.setTenantId(9999L);
        task.setStoreCode(STORE_CODE);
        task.setOrderId(12345L);
        task.setTaskType("BILL");
        task.setContent("[{\"text\":\"hello\",\"fontSize\":0,\"bold\":false,\"align\":\"LEFT\",\"type\":\"TEXT\"}]");
        task.setStatus(PrintTask.STATUS_PENDING);
        task.setTerminalId(terminalId);
        task.setTerminalCode(TERMINAL_CODE);
        task.setRetryCount(0);
        task.setCreatedTime(LocalDateTime.now());
        printTaskMapper.insertIgnoreTenant(task);
        Long taskId = task.getId();

        // 心跳拉取任务（PENDING → PULLED）
        MvcResult hb = mockMvc.perform(post("/printer/agent/heartbeat")
                        .header("X-Terminal-Code", TERMINAL_CODE)
                        .header("X-Terminal-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andReturn();
        JsonNode tasks = objectMapper.readTree(hb.getResponse().getContentAsString()).path("data");
        assertTrue(tasks.size() >= 1);
        assertEquals(PrintTask.STATUS_PULLED,
                printTaskMapper.findByIdIgnoreTenant(taskId).getStatus());

        // 打印回执成功
        mockMvc.perform(post("/printer/agent/task/" + taskId + "/callback")
                        .header("X-Terminal-Code", TERMINAL_CODE)
                        .header("X-Terminal-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"success\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertEquals(PrintTask.STATUS_SUCCESS, printTaskMapper.findByIdIgnoreTenant(taskId).getStatus());
    }

    @Test
    void testHeartbeatBadToken() throws Exception {
        register();
        mockMvc.perform(post("/printer/agent/heartbeat")
                        .header("X-Terminal-Code", TERMINAL_CODE)
                        .header("X-Terminal-Token", "wrong-token"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0));
    }
}
