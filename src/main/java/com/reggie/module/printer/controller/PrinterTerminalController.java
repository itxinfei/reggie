package com.reggie.module.printer.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.printer.model.PrintTerminal;
import com.reggie.module.printer.service.PrintTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 打印终端管理（后台）
 *
 * <p>门店 PC 打印代理的登记/启停/测试/删除。总部超管（roleKey=SUPER_ADMIN）可跨门店查看全部终端；
 * 门店员工仅能查看本门店终端。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@RestController
@RequestMapping("/printer/terminal")
@RequireEmployee
@Tag(name = "打印终端管理（门店PC打印代理）")
public class PrinterTerminalController {

    @Autowired
    private PrintTerminalService printTerminalService;

    /**
     * 终端分页：名称 / 终端编码模糊，门店编码精确，状态筛选。
     *
     * @return 分页结果（含在线状态字段 online）
     */
    @GetMapping("/page")
    @Operation(summary = "打印终端分页")
    public R<IPage<PrintTerminal>> page(@RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
                                        @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int pageSize,
                                        @RequestParam(required = false) @Parameter(description = "终端名称") String name,
                                        @RequestParam(required = false) @Parameter(description = "终端编码") String code,
                                        @RequestParam(required = false) @Parameter(description = "门店编码") String storeCode,
                                        @RequestParam(required = false) @Parameter(description = "状态 0/1") Integer status,
                                        HttpServletRequest request) {
        return R.success(printTerminalService.pageQuery(page, pageSize, resolveTenantId(request),
                name, code, storeCode, status));
    }

    /**
     * 终端统计：总数 / 在线（2分钟内心跳）/ 启用 / 停用。
     *
     * @return total / online / enabled / disabled
     */
    @GetMapping("/stats")
    @Operation(summary = "打印终端统计")
    public R<Map<String, Object>> stats(HttpServletRequest request) {
        return R.success(printTerminalService.statTerminals(resolveTenantId(request)));
    }

    /**
     * 启用 / 停用终端。停用后代理心跳不再领取任务。
     *
     * @param id     终端ID
     * @param status 0=停用 1=启用
     */
    @PutMapping("/status/{id}")
    @Operation(summary = "启用/停用终端")
    public R<Void> updateStatus(@PathVariable("id") @Parameter(description = "终端ID") Long id,
                                @RequestParam @Parameter(description = "0=停用 1=启用") Integer status,
                                HttpServletRequest request) {
        printTerminalService.updateStatus(id, status, resolveTenantId(request));
        return R.success(null);
    }

    /**
     * 测试打印：向指定终端派发一条 TEST 任务，代理收到后调用本地打印机。
     *
     * @param id 终端ID
     */
    @PostMapping("/test/{id}")
    @Operation(summary = "测试打印")
    public R<Void> testPrint(@PathVariable("id") @Parameter(description = "终端ID") Long id,
                             HttpServletRequest request) {
        boolean sent = printTerminalService.testPrint(id, resolveTenantId(request));
        return sent ? R.success(null) : R.error("终端不存在或已停用");
    }

    /**
     * 删除终端（仅停用终端可删，逻辑删除）。
     *
     * @param id 终端ID
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除终端")
    public R<Void> delete(@PathVariable("id") @Parameter(description = "终端ID") Long id,
                          HttpServletRequest request) {
        printTerminalService.deleteTerminal(id, resolveTenantId(request));
        return R.success(null);
    }

    /**
     * 租户视角：总部超管（roleKey=SUPER_ADMIN，与 EmployeeController.resolveRoleKey 对齐）看全部，
     * 门店员工看本门店租户。
     */
    private Long resolveTenantId(HttpServletRequest request) {
        String roleKey = (String) request.getAttribute("roleKey");
        return "SUPER_ADMIN".equals(roleKey) ? null : BaseContext.getCurrentTenantId();
    }
}
