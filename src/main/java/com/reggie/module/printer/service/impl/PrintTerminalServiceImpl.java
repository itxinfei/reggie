package com.reggie.module.printer.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.printer.dto.AgentRegisterDTO;
import com.reggie.module.printer.mapper.PrintTaskMapper;
import com.reggie.module.printer.mapper.PrintTerminalMapper;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.model.PrintTerminal;
import com.reggie.module.printer.service.PrintTerminalService;
import com.reggie.module.store.mapper.StoreInfoMapper;
import com.reggie.module.store.model.StoreInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 打印终端服务实现
 *
 * <p>代理端（register/heartbeat/callback）无登录会话，所有数据访问走
 * {@code @InterceptorIgnore(tenantLine = "true")} 的自定义 Mapper 方法并显式条件。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Slf4j
@Service
public class PrintTerminalServiceImpl extends ServiceImpl<PrintTerminalMapper, PrintTerminal>
        implements PrintTerminalService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 心跳拉取任务上限 */
    private static final int POLL_LIMIT = 10;

    @Autowired
    private PrintTerminalMapper printTerminalMapper;

    @Autowired
    private PrintTaskMapper printTaskMapper;

    @Autowired
    private StoreInfoMapper storeInfoMapper;

    @Override
    public Map<String, Object> register(AgentRegisterDTO dto) {
        StoreInfo store = storeInfoMapper.findByStoreCode(dto.getStoreCode());
        if (store == null) {
            throw new CustomException("门店编码不存在，请在后台门店管理中确认 store_code");
        }

        PrintTerminal exist = printTerminalMapper.findByTerminalCode(dto.getTerminalCode());
        String token = UUID.randomUUID().toString().replace("-", "");
        if (exist == null) {
            PrintTerminal terminal = new PrintTerminal();
            terminal.setTenantId(store.getTenantId() != null ? store.getTenantId() : 0L);
            terminal.setStoreCode(dto.getStoreCode());
            terminal.setTerminalCode(dto.getTerminalCode());
            terminal.setToken(token);
            terminal.setName(dto.getName() != null ? dto.getName() : dto.getTerminalCode());
            terminal.setPrinterName(dto.getPrinterName() != null ? dto.getPrinterName() : "");
            terminal.setPaperSize(normalizePaper(dto.getPaperSize()));
            // 默认接收全部打印类型（BILL/KITCHEN/DELIVERY/TEST），
            // 避免新终端只收 BILL 导致外卖单(DELIVERY)/后厨单(KITCHEN)不派发；
            // PrinterServiceImpl.matchPrintType 空串=接收全部
            terminal.setPrintTypes("");
            terminal.setStatus(0);
            terminal.setClientVersion(dto.getClientVersion() != null ? dto.getClientVersion() : "");
            terminal.setCreatedTime(LocalDateTime.now());
            terminal.setUpdateTime(LocalDateTime.now());
            // 代理端无会话，BaseMapper.insert 会被租户拦截器覆盖 tenant_id=-1，必须走自定义 INSERT
            printTerminalMapper.insertIgnoreTenant(terminal);
            log.info("[打印代理] 新终端注册: code={}, storeCode={}, name={}", dto.getTerminalCode(), dto.getStoreCode(), dto.getName());
            return buildRegisterResult(terminal);
        }

        // 已注册终端：刷新门店/打印机信息 + 重新签发 token（旧 token 立即失效）
        exist.setTenantId(store.getTenantId() != null ? store.getTenantId() : 0L);
        exist.setStoreCode(dto.getStoreCode());
        exist.setToken(token);
        if (dto.getName() != null && !dto.getName().isEmpty()) {
            exist.setName(dto.getName());
        }
        if (dto.getPrinterName() != null && !dto.getPrinterName().isEmpty()) {
            exist.setPrinterName(dto.getPrinterName());
        }
        exist.setPaperSize(normalizePaper(dto.getPaperSize()));
        exist.setClientVersion(dto.getClientVersion() != null ? dto.getClientVersion() : exist.getClientVersion());
        exist.setUpdateTime(LocalDateTime.now());
        // 代理端无会话，updateById 会被租户拦截器追加 tenant_id=-1 条件导致静默更新 0 行，必须走自定义 UPDATE
        printTerminalMapper.updateIgnoreTenant(exist);
        log.info("[打印代理] 终端重新注册: code={}, storeCode={}", dto.getTerminalCode(), dto.getStoreCode());
        return buildRegisterResult(exist);
    }

    @Override
    public List<PrintTask> heartbeat(String terminalCode, String token, String printerName, String version) {
        PrintTerminal terminal = authTerminal(terminalCode, token);
        LocalDateTime now = LocalDateTime.now();
        printTerminalMapper.updateHeartbeat(terminal.getId(),
                printerName != null ? printerName : terminal.getPrinterName(),
                version != null ? version : terminal.getClientVersion(), now);

        if (terminal.getStatus() == null || terminal.getStatus() != 1) {
            // 停用终端仅维持心跳（用于在线状态展示），不派发任务
            return new ArrayList<>();
        }

        List<PrintTask> tasks = printTaskMapper.listPending(terminal.getId(), POLL_LIMIT);
        for (PrintTask task : tasks) {
            printTaskMapper.markPulled(task.getId(), now);
        }
        if (!tasks.isEmpty()) {
            log.info("[打印代理] 终端 {} 领取任务 {} 条", terminalCode, tasks.size());
        }
        return tasks;
    }

    @Override
    public boolean callback(Long taskId, String terminalCode, String token, boolean success, String errorMsg) {
        PrintTerminal terminal = authTerminal(terminalCode, token);
        PrintTask task = printTaskMapper.findByIdIgnoreTenant(taskId);
        if (task == null) {
            log.warn("[打印代理] 回执任务不存在: taskId={}", taskId);
            return false;
        }
        if (!terminal.getId().equals(task.getTerminalId())) {
            log.warn("[打印代理] 回执终端不匹配: taskId={}, terminal={}", taskId, terminalCode);
            return false;
        }
        String status = success ? PrintTask.STATUS_SUCCESS : PrintTask.STATUS_FAILED;
        int rows = printTaskMapper.updateResult(taskId, status, success ? "" : (errorMsg != null ? errorMsg : "打印失败"),
                LocalDateTime.now());
        log.info("[打印代理] 任务回执: taskId={}, status={}, error={}", taskId, status, errorMsg);
        return rows > 0;
    }

    @Override
    public boolean testPrint(Long terminalId, Long tenantId) {
        PrintTerminal terminal = printTerminalMapper.findByIdIgnoreTenant(terminalId);
        if (terminal == null || (terminal.getStatus() == null || terminal.getStatus() != 1)) {
            return false;
        }
        checkTenant(terminal, tenantId);
        String content = "[{\"text\":\"=== 测试打印 ===\",\"fontSize\":2,\"bold\":true,"
                + "\"align\":\"CENTER\",\"type\":\"TEXT\"},"
                + "{\"text\":\"打印代理工作正常\",\"fontSize\":0,\"bold\":false,"
                + "\"align\":\"CENTER\",\"type\":\"TEXT\"},"
                + "{\"text\":\"" + LocalDateTime.now().format(DTF) + "\",\"fontSize\":0,\"bold\":false,"
                + "\"align\":\"CENTER\",\"type\":\"TEXT\"}]";
        PrintTask task = new PrintTask();
        task.setTenantId(terminal.getTenantId());
        task.setStoreCode(terminal.getStoreCode());
        task.setOrderId(null);
        task.setTaskType("TEST");
        task.setContent(content);
        task.setStatus(PrintTask.STATUS_PENDING);
        task.setTerminalId(terminal.getId());
        task.setTerminalCode(terminal.getTerminalCode());
        task.setRetryCount(0);
        task.setCreatedTime(LocalDateTime.now());
        // 超管向门店终端派发时需落门店租户，BaseMapper.insert 会被拦截器覆盖 tenant_id，走自定义 INSERT
        printTaskMapper.insertIgnoreTenant(task);
        log.info("[打印代理] 测试任务派发: terminal={}, taskId={}", terminalCodeText(terminal), task.getId());
        return true;
    }

    @Override
    public IPage<PrintTerminal> pageQuery(int page, int pageSize, Long tenantId, String name, String code,
                                          String storeCode, Integer status) {
        return printTerminalMapper.listPage(PageUtils.of(page, pageSize), tenantId, name, code, storeCode, status);
    }

    @Override
    public Map<String, Object> statTerminals(Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime onlineSince = LocalDateTime.now().minusMinutes(2);
        result.put("total", printTerminalMapper.countBy(tenantId, null));
        result.put("online", printTerminalMapper.countOnline(tenantId, onlineSince));
        result.put("enabled", printTerminalMapper.countBy(tenantId, 1));
        result.put("disabled", printTerminalMapper.countBy(tenantId, 0));
        return result;
    }

    @Override
    public boolean updateStatus(Long id, Integer status, Long tenantId) {
        PrintTerminal terminal = printTerminalMapper.findByIdIgnoreTenant(id);
        if (terminal == null) {
            throw new CustomException("终端不存在");
        }
        checkTenant(terminal, tenantId);
        if (status == null || (status != 0 && status != 1)) {
            throw new CustomException("非法状态值");
        }
        terminal.setStatus(status);
        terminal.setUpdateTime(LocalDateTime.now());
        return printTerminalMapper.updateIgnoreTenant(terminal) > 0;
    }

    @Override
    public boolean deleteTerminal(Long id, Long tenantId) {
        PrintTerminal terminal = printTerminalMapper.findByIdIgnoreTenant(id);
        if (terminal == null) {
            throw new CustomException("终端不存在");
        }
        checkTenant(terminal, tenantId);
        if (terminal.getStatus() != null && terminal.getStatus() == 1) {
            throw new CustomException("请先停用终端再删除");
        }
        return printTerminalMapper.deleteIgnoreTenant(id, LocalDateTime.now()) > 0;
    }

    /**
     * 代理鉴权：按 terminalCode 查终端并校验 token。
     */
    private PrintTerminal authTerminal(String terminalCode, String token) {
        PrintTerminal terminal = printTerminalMapper.findByTerminalCode(terminalCode);
        if (terminal == null) {
            throw new CustomException("终端未注册");
        }
        if (token == null || !token.equals(terminal.getToken())) {
            throw new CustomException("终端鉴权失败");
        }
        return terminal;
    }

    private Map<String, Object> buildRegisterResult(PrintTerminal terminal) {
        Map<String, Object> result = new HashMap<>();
        result.put("terminalId", terminal.getId());
        result.put("token", terminal.getToken());
        result.put("status", terminal.getStatus());
        result.put("serverTime", LocalDateTime.now().format(DTF));
        return result;
    }

    private String normalizePaper(String paperSize) {
        if (paperSize != null && paperSize.contains("58")) {
            return "58mm";
        }
        return "80mm";
    }

    private String terminalCodeText(PrintTerminal terminal) {
        return terminal != null ? terminal.getTerminalCode() : "?";
    }

    /**
     * 管理端租户归属校验：非超管（tenantId 非空）时终端必须属于当前租户。
     */
    private void checkTenant(PrintTerminal terminal, Long tenantId) {
        if (tenantId != null && !tenantId.equals(terminal.getTenantId())) {
            throw new CustomException("终端不属于当前门店");
        }
    }
}
