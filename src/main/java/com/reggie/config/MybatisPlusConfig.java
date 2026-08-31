package com.reggie.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.reggie.common.BaseContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * MyBatis-Plus配置类，配置多租户插件和分页插件。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 不需要租户隔离的表：
     * - tenant: 租户表本身不需要 tenant_id
     * - employee: 在 EmployeeController 中手动添加了租户过滤
     * - shopping_cart: 暂无 tenant_id 列，通过 userId 关联隔离
     * - ai_provider_config: 系统级AI大模型配置表，暂无 tenant_id 列，不需要租户隔离
     * - dish_evaluation: 暂无 tenant_id 列，通过 userId 关联隔离
     * - permission: 全局权限目录表，无 tenant_id 列
     * - role_permission: 角色-权限关联表，无 tenant_id 列
     * - region: 行政区划字典表（省/市/区），全局共享，无 tenant_id 列，通过 level 关联隔离
     * 修改点：permission/role_permission 两个表均无 tenant_id 列，若不加入忽略表，
     * TenantLineInnerInterceptor 会对其追加 WHERE tenant_id = ? 导致 Unknown column 异常。
     * 修改点：region 为全局行政区划字典表，无 tenant_id 列，加入白名单避免 Unknown column 异常。
     */
    private static final Set<String> IGNORE_TABLES = new HashSet<>(Arrays.asList(
        "tenant", "employee", "shopping_cart", "ai_provider_config", "dish_evaluation",
        "permission", "role_permission", "region"
    ));

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = BaseContext.getCurrentTenantId();
                if (tenantId == null) {
                    // fail-closed：租户上下文为空时返回无效ID，避免生成 tenant_id = null 的 SQL
                    // 正常业务接口均有登录态，租户上下文非空；告警逻辑见 ignoreTable 中的兜底 warn
                    return new LongValue(-1L);
                }
                return new LongValue(tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 仅忽略无 tenant_id 列的表，不跳过所有过滤
                // 修复说明：原实现在此处判断 tenantId == null 时返回 true（跳过过滤），
                // 与 getTenantId 的 fail-closed 逻辑自相矛盾，导致租户上下文为空时隔离完全失效。
                // 现修复为仅做白名单判断，租户为空时由 getTenantId 返回 -1L 兜底，实现 fail-closed。
                boolean ignored = IGNORE_TABLES.contains(tableName);
                if (!ignored && BaseContext.getCurrentTenantId() == null) {
                    // 兜底告警：表不在白名单但租户上下文为空，getTenantId 将返回 -1L 导致查询返回空集，
                    // 用于及时发现白名单漏网或匿名访问未走预期路径的表
                    log.warn("租户上下文为空且表[{}]不在白名单，SQL 将追加 tenant_id=-1（返回空集），请检查是否需加入白名单",
                            tableName);
                }
                return ignored;
            }
        }));
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        mybatisPlusInterceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return mybatisPlusInterceptor;
    }
}
