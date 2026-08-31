package com.reggie.module.printer.controller;

import com.reggie.common.R;
import com.reggie.module.printer.dto.AgentCallbackDTO;
import com.reggie.module.printer.dto.AgentRegisterDTO;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.service.PrintTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 打印代理接口（门店 PC 本地打印）
 *
 * <p>无登录会话的匿名接口（已入 {@link com.reggie.common.AuthConstants#LOGIN_EXCLUDE_URLS}
 * 与 {@link com.reggie.common.AuthConstants#CSRF_EXCLUDE_URLS}），通过终端唯一码 + token 鉴权。
 * 门店 PC 上的 Python 打印代理程序轮询调用 register / heartbeat / callback。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@RestController
@RequestMapping("/printer/agent")
@Tag(name = "打印代理（门店PC本地打印）")
public class PrinterAgentController {

    @Autowired
    private PrintTerminalService printTerminalService;

    /**
     * 代理注册：按终端唯一码登记/刷新终端，返回鉴权 token。
     *
     * @param dto 注册请求
     * @return terminalId / token / status / serverTime
     */
    @PostMapping("/register")
    @Operation(summary = "代理注册（登记终端并获取token）")
    public R<Map<String, Object>> register(@RequestBody @Valid AgentRegisterDTO dto) {
        return R.success(printTerminalService.register(dto));
    }

    /**
     * 代理心跳：刷新在线时间并拉取待打印任务（PENDING → PULLED）。
     * 建议心跳周期 30 秒；接口同步返回新任务。
     *
     * @param terminalCode 终端唯一码（请求头）
     * @param token        鉴权 token（请求头）
     * @param printerName  本机打印机名（变更时上报）
     * @param version      客户端版本
     * @return 待打印任务列表
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "代理心跳并拉取任务")
    public R<List<PrintTask>> heartbeat(
            @RequestHeader("X-Terminal-Code") @Parameter(description = "终端唯一码") String terminalCode,
            @RequestHeader("X-Terminal-Token") @Parameter(description = "鉴权token") String token,
            @RequestParam(value = "printerName", required = false) @Parameter(description = "本机打印机名")
                    String printerName,
            @RequestParam(value = "version", required = false) @Parameter(description = "客户端版本")
                    String version) {
        return R.success(printTerminalService.heartbeat(terminalCode, token, printerName, version));
    }

    /**
     * 任务回执：打印成功/失败后通知后端更新任务状态。
     *
     * @param taskId       任务ID
     * @param terminalCode 终端唯一码（请求头）
     * @param token        鉴权 token（请求头）
     * @param dto          执行结果
     * @return 受理结果
     */
    @PostMapping("/task/{id}/callback")
    @Operation(summary = "任务打印回执")
    public R<Void> callback(@PathVariable("id") @Parameter(description = "任务ID") Long taskId,
                            @RequestHeader("X-Terminal-Code") @Parameter(description = "终端唯一码")
                                    String terminalCode,
                            @RequestHeader("X-Terminal-Token") @Parameter(description = "鉴权token")
                                    String token,
                            @RequestBody AgentCallbackDTO dto) {
        boolean accepted = printTerminalService.callback(taskId, terminalCode, token,
                dto.getSuccess() != null && dto.getSuccess(), dto.getErrorMsg());
        return accepted ? R.success(null) : R.error("任务回执受理失败");
    }
}
