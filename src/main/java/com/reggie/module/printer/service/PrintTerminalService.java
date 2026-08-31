package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.dto.AgentRegisterDTO;
import com.reggie.module.printer.model.PrintTask;
import com.reggie.module.printer.model.PrintTerminal;

import java.util.List;
import java.util.Map;

/**
 * 打印终端服务
 *
 * <p>门店 PC 打印代理注册 / 心跳 / 回执（代理端），以及终端管理 / 测试打印（管理端）。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
public interface PrintTerminalService extends IService<PrintTerminal> {

    /**
     * 代理注册：按 terminalCode 查已有终端（更新门店/打印机信息并刷新 token），
     * 不存在则新建（默认停用，需管理员启用后才会收到任务）。
     *
     * @param dto 注册请求
     * @return terminalId / token / status / serverTime
     */
    Map<String, Object> register(AgentRegisterDTO dto);

    /**
     * 代理心跳：校验 token 并刷新在线时间；启用中的终端拉取待处理任务（PENDING → PULLED）。
     *
     * @param terminalCode 终端唯一码
     * @param token        鉴权 token
     * @param printerName  本机打印机名（可为空，保持原值）
     * @param version      客户端版本
     * @return 待打印任务列表（含 content JSON）
     */
    List<PrintTask> heartbeat(String terminalCode, String token, String printerName, String version);

    /**
     * 代理回执任务执行结果。
     *
     * @param taskId       任务ID
     * @param terminalCode 终端唯一码
     * @param token        鉴权 token
     * @param success      是否成功
     * @param errorMsg     失败原因
     * @return 是否受理
     */
    boolean callback(Long taskId, String terminalCode, String token, boolean success, String errorMsg);

    /**
     * 管理端测试打印：向指定终端派发一条 TEST 任务。
     *
     * @param terminalId 终端ID
     * @param tenantId   当前租户（超管可为空）
     * @return 是否派发成功（终端不存在/停用时失败）
     */
    boolean testPrint(Long terminalId, Long tenantId);

    /**
     * 终端分页（管理端）。
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @param tenantId  租户ID（超管为空=全部）
     * @param name      名称模糊
     * @param code      终端编码模糊
     * @param storeCode 门店编码精确
     * @param status    状态（0/1）
     * @return 分页结果
     */
    IPage<PrintTerminal> pageQuery(int page, int pageSize, Long tenantId, String name, String code,
                                   String storeCode, Integer status);

    /**
     * 终端统计（管理端）：总数 / 在线 / 启用 / 停用。
     *
     * @param tenantId 租户ID（超管为空=全部）
     * @return total / online / enabled / disabled
     */
    Map<String, Object> statTerminals(Long tenantId);

    /**
     * 启用 / 停用终端（管理端）。
     *
     * @param id       终端ID
     * @param status   0=停用 1=启用
     * @param tenantId 当前租户（超管为空）
     * @return 是否成功
     */
    boolean updateStatus(Long id, Integer status, Long tenantId);

    /**
     * 删除终端（管理端，仅停用终端可删）。
     *
     * @param id       终端ID
     * @param tenantId 当前租户（超管为空）
     * @return 是否成功
     */
    boolean deleteTerminal(Long id, Long tenantId);
}
