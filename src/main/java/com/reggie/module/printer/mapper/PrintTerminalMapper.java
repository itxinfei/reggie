package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.module.printer.model.PrintTerminal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印终端 Mapper
 *
 * <p>代理端接口（register/heartbeat/callback）无登录会话，租户上下文为空，
 * 若走租户拦截器会注入 tenant_id=-1 导致查不到数据，故代理端查询统一
 * 使用 {@code @InterceptorIgnore(tenantLine = "true")} 并显式条件。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Mapper
public interface PrintTerminalMapper extends BaseMapper<PrintTerminal> {

    /**
     * 按终端唯一码查询（代理端）
     *
     * @param code 终端唯一码
     * @return 终端
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM print_terminal WHERE terminal_code = #{code} AND is_deleted = 0")
    PrintTerminal findByTerminalCode(@Param("code") String code);

    /**
     * 更新心跳信息（代理端）
     *
     * @param id          终端ID
     * @param printerName 本机打印机名
     * @param version     客户端版本
     * @param now         心跳时间
     * @return 更新行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE print_terminal SET last_heartbeat = #{now}, printer_name = #{printerName}, "
            + "client_version = #{version} WHERE id = #{id} AND is_deleted = 0")
    int updateHeartbeat(@Param("id") Long id, @Param("printerName") String printerName,
                        @Param("version") String version, @Param("now") LocalDateTime now);

    /**
     * 注册终端（代理端）：绕过租户拦截器，显式写入解析出的门店租户。
     * <p>注意：BaseMapper.insert 会被 TenantLineInnerInterceptor 强制覆盖 tenant_id 为
     * 当前租户值（代理端无会话时为 -1），因此注册必须走此自定义 INSERT。</p>
     *
     * @param terminal 终端
     * @return 影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO print_terminal (tenant_id, store_code, terminal_code, token, name, printer_name, "
            + "paper_size, print_types, status, client_version, created_time, update_time, is_deleted) "
            + "VALUES (COALESCE(#{tenantId}, 0), COALESCE(#{storeCode}, ''), "
            + "COALESCE(#{terminalCode}, ''), COALESCE(#{token}, ''), COALESCE(#{name}, ''), "
            + "COALESCE(#{printerName}, ''), COALESCE(#{paperSize}, '80mm'), COALESCE(#{printTypes}, 'BILL'), "
            + "COALESCE(#{status}, 0), COALESCE(#{clientVersion}, ''), "
            + "COALESCE(#{createdTime}, CURRENT_TIMESTAMP), COALESCE(#{updateTime}, CURRENT_TIMESTAMP), 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnoreTenant(PrintTerminal terminal);

    /**
     * 更新终端（代理端/管理端共用）：绕过租户拦截器，按主键精确更新。
     * <p>包含 status 列：注册刷新时 status 传原值（保持启停状态不变），
     * 管理端启停时 status 传新值。注意实体其它字段为 null 时也会覆盖为 null，
     * 调用方须基于已查出的完整实体修改后回写。</p>
     *
     * @param terminal 终端
     * @return 影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE print_terminal SET tenant_id = #{tenantId}, store_code = #{storeCode}, "
            + "token = #{token}, name = #{name}, printer_name = #{printerName}, paper_size = #{paperSize}, "
            + "print_types = #{printTypes}, status = #{status}, client_version = #{clientVersion}, "
            + "update_time = #{updateTime} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int updateIgnoreTenant(PrintTerminal terminal);

    /**
     * 按主键查询（管理端按 id 操作）：绕过租户拦截器，租户归属由 Service 显式校验。
     *
     * @param id 终端ID
     * @return 终端
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM print_terminal WHERE id = #{id} AND is_deleted = 0")
    PrintTerminal findByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 逻辑删除（管理端）：绕过租户拦截器，租户归属由 Service 显式校验。
     *
     * @param id  终端ID
     * @param now 删除时间
     * @return 影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE print_terminal SET is_deleted = 1, update_time = #{now} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int deleteIgnoreTenant(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 终端分页（管理端）：绕过租户拦截器，tenantId 非空时显式过滤（门店员工），
     * 为空时查询全部（总部超管跨门店视角）。
     *
     * @param page      分页
     * @param tenantId  租户ID（可为 null=全部）
     * @param name      名称模糊
     * @param code      终端编码模糊
     * @param storeCode 门店编码精确
     * @param status    状态（0/1）
     * @return 分页结果
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT * FROM print_terminal WHERE is_deleted = 0 "
            + "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if> "
            + "<if test='name != null and name != \"\"'>AND name LIKE CONCAT('%', #{name}, '%')</if> "
            + "<if test='code != null and code != \"\"'>AND terminal_code LIKE CONCAT('%', #{code}, '%')</if> "
            + "<if test='storeCode != null and storeCode != \"\"'>AND store_code = #{storeCode}</if> "
            + "<if test='status != null'>AND status = #{status}</if> "
            + "ORDER BY id DESC</script>")
    IPage<PrintTerminal> listPage(IPage<PrintTerminal> page, @Param("tenantId") Long tenantId,
                                  @Param("name") String name, @Param("code") String code,
                                  @Param("storeCode") String storeCode, @Param("status") Integer status);

    /**
     * 终端计数（管理端统计）：绕过租户拦截器，tenantId 非空时显式过滤。
     *
     * @param tenantId 租户ID（可为 null=全部）
     * @param status   状态过滤（0/1，null=全部）
     * @return 计数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT COUNT(*) FROM print_terminal WHERE is_deleted = 0 "
            + "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if> "
            + "<if test='status != null'>AND status = #{status}</if></script>")
    long countBy(@Param("tenantId") Long tenantId, @Param("status") Integer status);

    /**
     * 在线终端计数（管理端统计）：最近 {@code onlineSince} 内有心跳视为在线。
     *
     * @param tenantId    租户ID（可为 null=全部）
     * @param onlineSince 在线判定时间点
     * @return 计数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT COUNT(*) FROM print_terminal WHERE is_deleted = 0 "
            + "AND last_heartbeat IS NOT NULL AND last_heartbeat &gt;= #{onlineSince} "
            + "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if></script>")
    long countOnline(@Param("tenantId") Long tenantId, @Param("onlineSince") LocalDateTime onlineSince);

    /**
     * 按租户查启用终端（订单自动打印派发）：绕过租户拦截器，显式按 tenantId 匹配。
     * 订单仅带 tenantId（门店即租户），通过租户定位门店下的 PC 打印代理终端。
     *
     * @param tenantId 订单租户ID
     * @return 启用终端列表
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM print_terminal WHERE tenant_id = #{tenantId} AND status = 1 AND is_deleted = 0")
    List<PrintTerminal> listEnabledByTenant(@Param("tenantId") Long tenantId);
}
